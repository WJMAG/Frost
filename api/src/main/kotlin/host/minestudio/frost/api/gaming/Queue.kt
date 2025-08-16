package net.minestom.jam

import host.minestudio.frost.api.gaming.Game
import it.unimi.dsi.fastutil.Pair
import it.unimi.dsi.fastutil.objects.Object2LongMap
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.adventure.audience.PacketGroupingAudience
import net.minestom.server.command.CommandManager
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.CommandExecutor
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.arguments.ArgumentLoop
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.condition.CommandCondition
import net.minestom.server.command.builder.condition.Conditions
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.timer.ExecutionType
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.entity.EntityFinder
import org.jetbrains.annotations.Unmodifiable
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import java.util.function.IntFunction
import java.util.function.Supplier

/**
 * @author https://github.com/Minestom/game-jam-template/blob/main/src/main/java/net/minestom/jam/Queue.java
 */
@JvmRecord
data class Queue(val players: MutableSet<UUID>, val isPrivate: Boolean) : PacketGroupingAudience {
    /**
     * The queue system consists of two types of queues: public queues and private queues.
     * <br></br>
     * Public queues are joined automatically when a player tries to queue with `/queue`. If there doesn't exist one,
     * it will be created. Private queues ("parties") can never be joined automatically, and are created with `/party`. Both queues allow `/invite <username(s)>`, where the player can invite any number of users.
     */
    class Manager(private val instance: Instance, private val pos: Pos) {
        private val privateQueues: MutableList<Queue> = ArrayList<Queue>()
        private val publicQueues: MutableList<Queue> = ArrayList<Queue>()
        private val queueMembership: MutableMap<UUID?, Queue> = HashMap<UUID?, Queue>()
        private val invites: Object2LongMap<Pair<UUID?, UUID?>?> = Object2LongOpenHashMap<Pair<UUID?, UUID?>?>()


        fun joinPublicQueueWithMessages(player: Player) {
            val uuid = player.uuid
            val success = joinPublicQueue(uuid)

            val queue = getQueue(uuid)

            if (success) {
                queue!!.sendMessage(PLAYER_JOINED_QUEUE.apply(player.username)!!.append(queue.memberCount()!!))
            } else {
                player.sendMessage(ALREADY_QUEUED.append(queue!!.memberCount()!!))
            }
        }

        fun joinPublicQueue(uuid: UUID): Boolean {
            if (isQueued(uuid)) return false

            addToQueue(nextPublicQueue(), uuid)

            return true
        }

        fun createPrivateQueueWithMessages(player: Player): Boolean {
            val uuid = player.uuid
            val success = createPrivateQueue(uuid)

            player.sendMessage((if (success) CREATED_PRIVATE_QUEUE else ALREADY_QUEUED).append(getQueue(uuid)!!.memberCount()!!))

            return success
        }

        fun createPrivateQueue(uuid: UUID): Boolean {
            if (isQueued(uuid)) return false

            val queue = createPrivateQueue()
            addToQueue(queue, uuid)

            return true
        }

        fun dequeueWithMessages(player: Player) {
            val leftQueue = dequeue(player)

            player.sendMessage(if (leftQueue != null) LEFT_QUEUE else NOT_IN_QUEUE)
        }

        fun dequeue(player: Player): Queue? {
            val uuid = player.uuid

            val queue = getQueue(uuid)

            if (queue == null) return null

            // Remove the player internally
            queueMembership.remove(uuid)
            queue.players.remove(uuid)

            // Send messages to every other player on the team
            queue.sendMessage(PLAYER_LEFT_QUEUE.apply(player.username)!!.append(queue.memberCount()!!))

            return queue
        }

        fun invitePlayers(inviter: Player, invitees: MutableSet<Player>): Boolean {
            val uuid = inviter.uuid
            val queue = getQueue(uuid)

            if (queue == null) {
                inviter.sendMessage(MUST_BE_IN_A_QUEUE_TO_INVITE)
                return false
            }

            inviter.sendMessage(INVITED_PLAYERS.apply(invitees.size)!!.append(queue.memberCount()!!))

            for (invitee in invitees) {
                sendInvite(inviter, invitee, queue)
            }

            return true
        }

        private fun sendInvite(inviter: Player, invitee: Player, queue: Queue): Boolean {
            if (inviter.uuid == invitee.uuid) {
                inviter.sendMessage(CANNOT_INVITE_YOURSELF)
                return false
            } else if (queue.players.contains(invitee.uuid)) {
                inviter.sendMessage(ALREADY_IN_PARTY.apply(invitee.username)!!)
                return false
            }

            val currentTime = System.currentTimeMillis()

            val key = Pair.of<UUID?, UUID?>(inviter.uuid, invitee.uuid)
            val lastInvite = invites.getLong(key)

            if (currentTime - lastInvite > INVITE_EXPIRE_AFTER_MS) {
                val name = inviter.username

                invitee.sendMessage((if (queue.isPrivate) INVITED_PRIVATE_QUEUE else INVITED_PUBLIC_QUEUE).apply(name)!!)
                invitee.sendMessage(CLICK_TO_ACCEPT_INVITE.apply(name)!!)

                invites.put(key, currentTime)
                return true
            } else {
                inviter.sendMessage(ALREADY_INVITED.apply(invitee.username)!!)
                return false
            }
        }

        fun acceptWithMessages(player: Player, allegedInviter: Player): Boolean {
            val pair = Pair.of<UUID?, UUID?>(allegedInviter.uuid, player.uuid)
            val lastInvite = invites.getLong(pair)

            if (lastInvite == 0L) {
                player.sendMessage(HAS_NOT_INVITED.apply(allegedInviter.username)!!)
            } else if (System.currentTimeMillis() - lastInvite > INVITE_EXPIRE_AFTER_MS) {
                player.sendMessage(INVITE_HAS_EXPIRED.apply(allegedInviter.username)!!)
            } else if (isQueued(player.uuid)) {
                player.sendMessage(ALREADY_QUEUED)
            } else if (!isQueued(allegedInviter.uuid)) {
                player.sendMessage(INVITER_IS_NOT_QUEUED.apply(allegedInviter.username)!!)
            } else {
                invites.removeLong(pair)

                val queue: Queue = queueMembership[allegedInviter.uuid]!!

                addToQueue(queue, player.uuid)
                queue.sendMessage(PLAYER_JOINED_QUEUE.apply(player.username)!!.append(queue.memberCount()!!))

                return true
            }

            return false
        }

        /***
         * Returns whether or not the given player is currently queued.
         */
        fun isQueued(player: UUID): Boolean {
            return queueMembership.containsKey(player)
        }

        /**
         * Returns the queue that the player is in, or null if there does not exist one.
         */
        fun getQueue(player: UUID): Queue? {
            return queueMembership[player]
        }

        /**
         * Gets a public queue that a player can join.
         */
        private fun nextPublicQueue(): Queue {
            // Find a non-full queue
            for (queue in publicQueues) {
                if (queue.players.size < MAX_SIZE) {
                    return queue
                }
            }

            // Return an empty queue
            val queue = Queue(CopyOnWriteArraySet<UUID>(), false)
            publicQueues.add(queue)
            return queue
        }

        /**
         * Always creates a new private queue specifically for the player.
         */
        private fun createPrivateQueue(): Queue {
            val queue = Queue(CopyOnWriteArraySet<UUID>(), true)
            privateQueues.add(queue)
            return queue
        }

        /***
         * Attempts to add a player to a queue.
         */
        private fun addToQueue(queue: Queue, player: UUID) {
            queue.players.add(player)
            queueMembership.put(player, queue)

            if (queue.players.size < MAX_SIZE) return

            val counter = AtomicInteger(GAME_START_DELAY + 1) // one second before actually starting
            MinecraftServer.getSchedulerManager().submitTask(Supplier {
                if (queue.players.size < MAX_SIZE) return@Supplier TaskSchedule.stop()
                val time = counter.getAndDecrement()
                if (time > GAME_START_DELAY) return@Supplier TaskSchedule.seconds(1)

                if (time > 0) {
                    queue.sendMessage(GAME_STARTING_IN.apply(time)!!)
                    return@Supplier TaskSchedule.seconds(1)
                }

                // Start a new game
                queue.sendMessage(STARTING_GAME)
                Game(queue.players, instance, pos)

                // Remove the queue
                (if (queue.isPrivate) privateQueues else publicQueues).remove(queue)
                for (member in queue.players) {
                    queueMembership.remove(member)
                }
                queue.players.clear() // Clear queue just in case
                TaskSchedule.stop()
            }, ExecutionType.TICK_END)
        }
    }

    /**
     * Commands relevant to the [Manager].
     */
    @JvmRecord
    data class Commands(val manager: Manager) {
        /**
         * Tries to join a public queue, creating one if it does not exist.
         */
        inner class JoinQueue : Command("queue") {
            init {
                condition = Conditions.all(
                    { sender: CommandSender?, commandString: String? ->
                        Conditions.playerOnly(
                            sender,
                            commandString
                        )
                    },
                    NOT_IN_GAME
                )

                defaultExecutor = CommandExecutor { sender: CommandSender?, context: CommandContext? ->
                    val player = sender as Player
                    manager.joinPublicQueueWithMessages(player)
                }
            }
        }

        /**
         * Creates a private queue, optionally inviting some users.
         */
        inner class Party : Command("party") {
            init {
                condition = Conditions.all(
                    { sender: CommandSender?, commandString: String? ->
                        Conditions.playerOnly(
                            sender,
                            commandString
                        )
                    },
                    NOT_IN_GAME
                )

                defaultExecutor = CommandExecutor { sender: CommandSender?, context: CommandContext? ->
                    manager.createPrivateQueueWithMessages(
                        sender as Player
                    )
                }
                addSyntax({ sender: CommandSender?, context: CommandContext? ->
                    val player = sender as Player
                    if (manager.createPrivateQueueWithMessages(player)) {
                        val finders = context!!.get(PLAYERS).filterNotNull().toMutableList()
                        manager.invitePlayers(
                            player, coalescePlayers(
                                player, finders
                            )
                        )
                    }
                }, PLAYERS)
            }
        }

        /**
         * Tries to invite the provided user(s) to the current private or public queue.
         */
        inner class Invite : Command("invite") {
            init {
                condition = Conditions.all(
                    { sender: CommandSender?, commandString: String? ->
                        Conditions.playerOnly(
                            sender,
                            commandString
                        )
                    },
                    NOT_IN_GAME
                )

                defaultExecutor = CommandExecutor { sender: CommandSender?, context: CommandContext? ->
                    sender!!.sendMessage(
                        INVITE_SYNTAX
                    )
                }

                addSyntax({ sender: CommandSender?, context: CommandContext? ->
                    val player = sender as Player
                    val finders = context!!.get(PLAYERS).filterNotNull().toMutableList()
                    manager.invitePlayers(
                        player, coalescePlayers(
                            player, finders
                        )
                    )
                }, PLAYERS)
            }
        }

        /**
         * Leaves the current queue, if possible.
         */
        inner class Leave : Command("leave", "dequeue") {
            init {
                condition = Conditions.all(
                    { sender: CommandSender?, commandString: String? ->
                        Conditions.playerOnly(
                            sender,
                            commandString
                        )
                    },
                    NOT_IN_GAME
                )

                defaultExecutor = CommandExecutor { sender: CommandSender?, context: CommandContext? ->
                    val player = sender as Player
                    manager.dequeueWithMessages(player)
                }
            }
        }

        /**
         * Accepts a player's invite to another queue.
         */
        inner class Accept : Command("accept") {
            init {
                condition = Conditions.all(
                    { sender: CommandSender?, commandString: String? ->
                        Conditions.playerOnly(
                            sender,
                            commandString
                        )
                    },
                    NOT_IN_GAME
                )

                defaultExecutor = CommandExecutor { sender: CommandSender?, context: CommandContext? ->
                    sender!!.sendMessage(
                        ACCEPT_SYNTAX
                    )
                }

                addSyntax(CommandExecutor { sender: CommandSender?, context: CommandContext? ->
                    val player = sender as Player
                    val invited = coalescePlayer(player, context!!.get<EntityFinder?>(PLAYER)!!)

                    if (invited == null) {
                        player.sendMessage(UNKNOWN_PLAYER)
                        return@CommandExecutor
                    }
                    manager.acceptWithMessages(player, invited)
                }, PLAYER)
            }
        }

        companion object {
            /**
             * Registers every handler to a given queue manager and command manager.
             * The command manager should probably be `MinecraftServer.getCommandManager()`.
             */
            fun register(queues: Manager, manager: CommandManager) {
                val commands = Commands(queues)

                manager.register(
                    commands.JoinQueue(),
                    commands.Party(),
                    commands.Invite(),
                    commands.Leave(),
                    commands.Accept()
                )
            }

            private val NOT_IN_GAME = CommandCondition { sender: CommandSender?, commandString: String? ->
                if (commandString == null) return@CommandCondition true
                if (sender is Player && sender.hasTag(Game.GAME)) {
                    sender.sendMessage(CANNOT_QUEUE_IN_GAME)
                    return@CommandCondition false
                } else return@CommandCondition true
            }

            private val PLAYER: Argument<EntityFinder?> =
                ArgumentType.Entity("player").onlyPlayers(true).singleEntity(true)

            private val PLAYERS: ArgumentLoop<EntityFinder?> = ArgumentType.Loop<EntityFinder?>("players", PLAYER)

            private fun coalescePlayer(player: Player, finder: EntityFinder): Player? {
                val found = finder.find(player)

                return if (found.isEmpty()) null else found.first() as Player?
            }

            private fun coalescePlayers(player: Player, finders: MutableList<EntityFinder>): MutableSet<Player> {
                val players: MutableSet<Player> = HashSet<Player>()

                for (finder in finders) {
                    val invited = coalescePlayer(player, finder)

                    if (invited != null) players.add(invited)
                }

                return players
            }
        }
    }

    override fun getPlayers(): @Unmodifiable MutableCollection<Player> {
        return players.mapNotNull { uuid -> MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid) }
            .toMutableSet()
    }

    private fun memberCount(): Component? {
        return MEMBER_COUNT.apply(players.size)
    }

    companion object {
        /**
         * The maximum size of queues before a game starts.
         */
        const val MAX_SIZE: Int = 8

        /**
         * The number of milliseconds after which invites expire.
         */
        const val INVITE_EXPIRE_AFTER_MS: Long = 60000

        /**
         * The number of seconds it takes for a game to start after a queue is full.
         */
        const val GAME_START_DELAY: Int = 3

        private val ALREADY_QUEUED: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" You are already in a queue! Leave it with /leave!", NamedTextColor.RED)
        )

        private val CREATED_PRIVATE_QUEUE: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.GREEN, TextDecoration.BOLD),
            Component.text(" You created a new ", NamedTextColor.GRAY),
            Component.text("private", NamedTextColor.DARK_PURPLE),
            Component.text(" queue!", NamedTextColor.GRAY)
        )

        private val MUST_BE_IN_A_QUEUE_TO_INVITE: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" You must be in a queue to invite players!", NamedTextColor.RED)
        )

        private val PLAYERS_SUFFIX_PLURAL: Component = Component.text(" players!", NamedTextColor.GRAY)

        private val PLAYERS_SUFFIX_SINGULAR: Component = Component.text(" player!", NamedTextColor.GRAY)

        private val INVITED_PLAYERS: IntFunction<Component?> = IntFunction { players: Int ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.GREEN, TextDecoration.BOLD),
                Component.text(" Invited ", NamedTextColor.GRAY),
                Component.text(players, NamedTextColor.WHITE),
                if (players == 1) PLAYERS_SUFFIX_SINGULAR else PLAYERS_SUFFIX_PLURAL
            )
        }

        private val MEMBER_COUNT: IntFunction<Component?> = IntFunction { count: Int ->
            Component.textOfChildren(
                Component.text(" (", NamedTextColor.GRAY),
                Component.text(count, NamedTextColor.GRAY),
                Component.text("/$MAX_SIZE)", NamedTextColor.GRAY)
            )
        }

        private val NOT_IN_QUEUE: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" You are not in a queue!", NamedTextColor.RED)
        )

        private val LEFT_QUEUE: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.GREEN, TextDecoration.BOLD),
            Component.text(" You left the queue!", NamedTextColor.GRAY)
        )

        private val PLAYER_JOINED_QUEUE: Function<String?, Component?> = Function { username: String? ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text(" $username", NamedTextColor.WHITE),
                Component.text(" joined the queue!", NamedTextColor.GRAY)
            )
        }

        private val PLAYER_LEFT_QUEUE: Function<String?, Component?> = Function { username: String? ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text(" $username", NamedTextColor.WHITE),
                Component.text(" left the queue!", NamedTextColor.GRAY)
            )
        }

        private val INVITED_PUBLIC_QUEUE: Function<String?, Component?> = Function { username: String? ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text(" $username", NamedTextColor.WHITE),
                Component.text(" has invited you to join their ", NamedTextColor.GRAY),
                Component.text("public", NamedTextColor.LIGHT_PURPLE),
                Component.text(" queue!", NamedTextColor.GRAY)
            )
        }

        private val INVITED_PRIVATE_QUEUE: Function<String?, Component?> = Function { username: String? ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text(" $username", NamedTextColor.WHITE),
                Component.text(" has invited you to join their ", NamedTextColor.GRAY),
                Component.text("private", NamedTextColor.DARK_PURPLE),
                Component.text(" queue!", NamedTextColor.GRAY)
            )
        }

        private val CLICK_TO_ACCEPT_INVITE: Function<String?, Component?> = Function { username: String? ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text(" Click here or run ", NamedTextColor.GRAY),
                Component.text("/accept $username", NamedTextColor.WHITE),
                Component.text(" to accept!", NamedTextColor.GRAY)
            ).clickEvent(ClickEvent.runCommand("/accept $username"))
        }

        private val ALREADY_INVITED: Function<String?, Component?> = Function { username: String? ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text(" You have already invited ", NamedTextColor.RED),
                Component.text(username!!, NamedTextColor.RED),
                Component.text(" to your queue!", NamedTextColor.RED)
            )
        }

        private val CANNOT_INVITE_YOURSELF: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" You cannot invite yourself!", NamedTextColor.RED)
        )

        private val ALREADY_IN_PARTY: Function<String?, Component?> = Function { username: String? ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text(" ", NamedTextColor.RED),
                Component.text(username!!, NamedTextColor.RED),
                Component.text(" is already in your queue!", NamedTextColor.RED)
            )
        }

        private val HAS_NOT_INVITED: Function<String?, Component?> = Function { username: String? ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text(" ", NamedTextColor.RED),
                Component.text(username!!, NamedTextColor.RED),
                Component.text(" has not invited you to their queue!", NamedTextColor.RED)
            )
        }

        private val INVITE_HAS_EXPIRED: Function<String?, Component?> = Function { username: String? ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text(" The invite from ", NamedTextColor.RED),
                Component.text(username!!, NamedTextColor.RED),
                Component.text(" has expired!", NamedTextColor.RED)
            )
        }

        private val INVITER_IS_NOT_QUEUED: Function<String?, Component?> = Function { username: String? ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text(" ", NamedTextColor.RED),
                Component.text(username!!, NamedTextColor.RED),
                Component.text(" is not currently in a queue!", NamedTextColor.RED)
            )
        }

        private val GAME_STARTING_IN: IntFunction<Component?> = IntFunction { seconds: Int ->
            Component.textOfChildren(
                Component.text("[!]", NamedTextColor.GREEN, TextDecoration.BOLD),
                Component.text(" Game starting in ", NamedTextColor.GRAY),
                Component.text(seconds, NamedTextColor.WHITE),
                Component.text(if (seconds != 1) " seconds!" else " second!", NamedTextColor.GRAY)
            )
        }

        private val STARTING_GAME: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.GREEN, TextDecoration.BOLD),
            Component.text(" Starting game!", NamedTextColor.GRAY)
        )

        private val CANNOT_QUEUE_IN_GAME: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" You cannot run any queue commands as you are in a game!", NamedTextColor.RED)
        )

        private val UNKNOWN_PLAYER: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" Unknown player!", NamedTextColor.RED)
        )

        private val ACCEPT_SYNTAX: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" /accept syntax: /accept <player>", NamedTextColor.RED)
        )

        private val INVITE_SYNTAX: Component = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" /invite syntax: /invite <player(s)>", NamedTextColor.RED)
        )
    }
}