package org.action;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.Main;
import org.database.dto.*;
import org.exception.CustomException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.parsing.ChatCommand;
import org.parsing.Modifier;
import org.utility.Check;
import org.utility.Helper;
import org.utility.ProcessingContext;
import org.utility.CommunicationStream;

import java.util.*;

public class Index extends ActionHandler {
    private static final Map<String, Index.IndexingJob> INDEXING_JOBS;

    static {
        INDEXING_JOBS = Collections.synchronizedMap(new HashMap<>());

        ActionHandler.ACTION_MODIFIERS.put(
                Index.ActionModifier.CHANNEL,
                new Modifier<>(Helper.EmptyEnum.class, null, true, false, false, false, null, null)
        );
    }

    private static void addIndexingJob(String snowflakeGuild, MessageChannel sourceChannel, List<MessageChannel> serverChannelsToIndex) {
        Index.INDEXING_JOBS.put(snowflakeGuild, new Index.IndexingJob(sourceChannel, serverChannelsToIndex));
    }

    private static void markIndexingOfChannelDone(MessageChannel channelToMark, String snowflakeGuild) {
        Index.IndexingJob indexingJob = Index.INDEXING_JOBS.get(snowflakeGuild);
        synchronized (indexingJob) {
            Index.IndexingJob.IndexedChannel channel = indexingJob.findChannel(channelToMark.getId());
            channel.setIndexingFinished();
        }
    }

    private static boolean indexingFinished(String snowflakeGuild) {
        Index.IndexingJob indexingJob = Index.INDEXING_JOBS.get(snowflakeGuild);
        synchronized (indexingJob) {
            return indexingJob.indexingFinished();
        }
    }

    private static void finalizeIndexing(String snowflakeGuild) {
        IndexingJob finishedIndexingJob = Index.INDEXING_JOBS.remove(snowflakeGuild);

        MessageChannel sourceChannel = finishedIndexingJob.sourceChannel;
        String stringifiedChannels = Helper.stringifyCollection(finishedIndexingJob.indexedChannels, Index.IndexingJob.IndexedChannel::getChannelName, true);

        CommunicationStream.returnResponse(sourceChannel, "Indexing finished for channels: " + stringifiedChannels);
    }

    private static EmojiDto retrieveEmojiDto(MessageReaction reaction, String snowflakeGuild, Session session) {
        EmojiDto emojiDto;

        if (Helper.isGuildEmoji(reaction) && !GuildEmojiDto.guildEmojiExists(reaction.getEmoji().getName(), snowflakeGuild, session)) {
            emojiDto = new EmojiDto(reaction.getEmoji().getName());
            session.persist(emojiDto);

            CustomEmoji guildReaction = (CustomEmoji)reaction.getEmoji();
            GuildEmojiDto guildEmojiDto = new GuildEmojiDto(
                    emojiDto.getIdEmoji(), reaction.getGuild().getId(), guildReaction.getId(), guildReaction.isAnimated()
            );
            session.persist(guildEmojiDto);
        } else if (!EmojiDto.emojiExists(reaction.getEmoji().getName(), session)) {
            emojiDto = new EmojiDto(reaction.getEmoji().getName());
            session.persist(emojiDto);
        } else {
            // Emoji already exists
            if (Helper.isGuildEmoji(reaction)) {
                GuildEmojiDto guildEmojiDto = GuildEmojiDto.getGuildEmojiDtoByEmoji(reaction.getEmoji().getName(), snowflakeGuild, session);
                emojiDto = EmojiDto.getEmojiDtoByIdEmoji(guildEmojiDto.getIdEmoji(), session);
            } else {
                List<EmojiDto> emojis = EmojiDto.getEmojiDtosByEmoji(reaction.getEmoji().getName(), session);
                assert emojis.size() == 1;
                emojiDto = emojis.getFirst();
            }
        }

        return emojiDto;
    }

    private static void processReaction(Message message, MessageReaction reaction, List<User> users) {
        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            EmojiDto emojiDto = Index.retrieveEmojiDto(reaction, message.getGuild().getId(), session);

            for (User user : users) {
                UserDto.refreshUser(user.getId(), user.isBot(), session);
                ReactionDto reactionDto = new ReactionDto(message.getId(), user.getId(), emojiDto.getIdEmoji());
                session.persist(reactionDto);
            }
        } finally {
            transaction.commit();
            session.close();
        }
    }

    private static void processMessages(TextChannelMessages channelMessages) {
        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            for (Message message : channelMessages.messages) {
                MessageDto messageDto = new MessageDto(message.getId(), message.getAuthor().getId(), channelMessages.messageChannel.getId(), message.getContentRaw());
                session.persist(messageDto);
                session.flush();

                List<MessageReaction> reactions = message.getReactions();
                reactions.forEach(reaction -> reaction.retrieveUsers().queue(users -> {
                    if (!users.isEmpty()) {
                        Index.processReaction(message, reaction, users);
                    }
                }));
            }
        } finally {
            transaction.commit();
            session.close();
        }

        Message latestProcessedMessage = channelMessages.messages.getLast();
        channelMessages.messageChannel.getHistoryAfter(latestProcessedMessage.getId(), 100).queue(response -> {
            List<Message> messages = new ArrayList<>(response.getRetrievedHistory());
            if (!messages.isEmpty()) {
                Collections.reverse(messages);
                Index.processMessages(new Index.TextChannelMessages(
                        channelMessages.snowflakeGuild, channelMessages.messageChannel, messages)
                );
            } else {
                // No more messages to process
                Index.markIndexingOfChannelDone(channelMessages.messageChannel, channelMessages.snowflakeGuild);
                if (Index.indexingFinished(channelMessages.snowflakeGuild)) {
                    Index.finalizeIndexing(channelMessages.snowflakeGuild);
                }
            }
        });
    }

    private static void indexChannel(MessageChannel channel, String snowflakeGuild) {
        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            MessageDto.purgeChannelData(channel.getId(), snowflakeGuild, session);
        } finally {
            transaction.commit();
            session.close();
        }


        channel.getHistory().retrievePast(1).queue(newestMessages -> {
            if (!newestMessages.isEmpty()) {
                channel.getHistoryFromBeginning(100).queue(response -> {
                    List<Message> messages = new ArrayList<>(response.getRetrievedHistory());
                    Collections.reverse(messages);
                    Index.processMessages(new Index.TextChannelMessages(snowflakeGuild, channel, messages));
                });
            } else {
                // Channel is empty
                Index.markIndexingOfChannelDone(channel, snowflakeGuild);
                if (Index.indexingFinished(snowflakeGuild)) {
                    Index.finalizeIndexing(snowflakeGuild);
                }
            }
        });
    }

    private static List<MessageChannel> retrieveServerMessageChannels(MessageReceivedEvent event) {
        List<GuildChannel> guildChannels = event.getGuild().getChannels();
        List<MessageChannel> messageChannels = new ArrayList<>();

        guildChannels.forEach(guildChannel -> {
            if (guildChannel instanceof MessageChannel messageChannel) {
                messageChannels.add(messageChannel);
            }

            if (guildChannel instanceof IThreadContainer threadContainer) {
                List<ThreadChannel> threadChannels = threadContainer.getThreadChannels();
                messageChannels.addAll(threadChannels);
            }
        });

        return messageChannels;
    }

    private static boolean channelsExist(List<String> channelsIds, List<MessageChannel> serverChannels) {
        List<String> serverChannelsIds = serverChannels.stream()
                .map(Channel::getId)
                .toList();
        return serverChannelsIds.containsAll(channelsIds);
    }

    @Override
    public boolean mentionsAllowed() {
        return true;
    }

    @Override
    public void executeAction(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {
        if (Index.INDEXING_JOBS.containsKey(event.getGuild().getId())) {
            processingContext.addMessages("Indexing is already in progress for this server", ProcessingContext.MessageType.ERROR);
            return;
        }

        try {
            final ProcessingContext dummy = new ProcessingContext();
            List<String> idsOfChannelsToIndex = chatCommand.getArguments(Index.ActionModifier.CHANNEL, true, true, dummy).stream()
                    .map(argument -> Helper.extractIdFromChannelMention(argument.getTrimmedNormalizedLowercaseUsedValue(processingContext, "channel name")))
                    .toList();

            List<MessageChannel> serverChannels = Index.retrieveServerMessageChannels(event);
            boolean channelsExist = Index.channelsExist(idsOfChannelsToIndex, serverChannels);
            Check.isBooleanTrue(
                    channelsExist, true, "", "One or more unknown channels found"
            );

            List<MessageChannel> serverChannelsToIndex;
            if (idsOfChannelsToIndex.isEmpty()) {
                serverChannelsToIndex = serverChannels;
                processingContext.addMessages("No channels specified, indexing all channels", ProcessingContext.MessageType.WARNING);
            } else {
                serverChannelsToIndex = serverChannels.stream()
                        .filter(serverChannel -> idsOfChannelsToIndex.contains(serverChannel.getId()))
                        .toList();
            }

            Index.addIndexingJob(event.getGuild().getId(), event.getChannel(), serverChannelsToIndex);
            serverChannelsToIndex.forEach(channel -> Index.indexChannel(channel, event.getGuild().getId()));
            processingContext.addMessages(
                    "Indexing has been started for channels: " + Helper.stringifyCollection(serverChannelsToIndex, MessageChannel::getName, true),
                    ProcessingContext.MessageType.INFO_RESULT
            );
        } catch (CustomException exception) {
            processingContext.addMessages(exception.getMessage(), ProcessingContext.MessageType.ERROR);
        }
    }

    @Override
    protected Class<? extends Enum<?>> getModifierEnumClass() {
        return Index.ActionModifier.class;
    }

    public enum ActionModifier { CHANNEL }

    private record TextChannelMessages(String snowflakeGuild, MessageChannel messageChannel, List<Message> messages) {}

    private static class IndexingJob {
        private final MessageChannel sourceChannel;
        private final List<IndexedChannel> indexedChannels;

        public IndexingJob(MessageChannel sourceChannel, List<MessageChannel> channelsToIndex) {
            this.sourceChannel = sourceChannel;
            this.indexedChannels = channelsToIndex.stream().map(IndexedChannel::new).toList();
        }

        public IndexedChannel findChannel(String snowflakeChannel) {
            return this.indexedChannels.stream()
                    .filter(indexedChannel -> indexedChannel.messageChannel.getId().equals(snowflakeChannel))
                    .findFirst()
                    .orElseThrow();
        }

        public boolean indexingFinished() {
            return this.indexedChannels.stream().allMatch(indexedChannel -> indexedChannel.indexingFinished);
        }

        public static class IndexedChannel {
            private final MessageChannel messageChannel;
            private boolean indexingFinished;

            public IndexedChannel(MessageChannel messageChannel) {
                this.messageChannel = messageChannel;
            }

            public void setIndexingFinished() {
                if (this.indexingFinished) {
                    throw new IllegalStateException("Indexing has already been finished for this channel");
                }

                this.indexingFinished = true;
            }

            public String getChannelName() {
                return this.messageChannel.getName();
            }
        }
    }
}
