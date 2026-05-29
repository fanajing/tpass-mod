package org.fanajing.tpass;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.fanajing.tpass.team.TeamData;
import org.fanajing.tpass.team.TeamManager;
import org.fanajing.tpass.team.TeamStorage;
import org.fanajing.tpass.teleport.TeleportManager;
import org.fanajing.tpass.FlightCardManager;

import java.nio.file.Paths;
import java.util.UUID;

@Mod("tpass")
public class Tpass {

    public Tpass() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> buildTeamCommands() {
        return Commands.literal("team")
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String name = StringArgumentType.getString(context, "name");
                        if (TeamManager.createTeam(name, player)) {
                            player.sendSystemMessage(Component.literal("队伍 [" + name + "] 创建成功"));
                        } else {
                            player.sendSystemMessage(Component.literal("创建失败：队伍名已存在或你已在其他队伍中"));
                        }
                        return 1;
                    })
                )
            )
            .then(Commands.literal("invite")
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        ServerPlayer target = EntityArgument.getPlayer(context, "target");
                        TeamData team = TeamManager.getPlayerTeam(player);
                        if (team == null) {
                            player.sendSystemMessage(Component.literal("你不在任何队伍中"));
                            return 0;
                        }
                        if (!team.leader.equals(player.getUUID())) {
                            player.sendSystemMessage(Component.literal("只有队长可以邀请玩家"));
                            return 0;
                        }
                        if (TeamManager.isInTeam(target)) {
                            player.sendSystemMessage(Component.literal("该玩家已在其他队伍中"));
                            return 0;
                        }
                        TeamManager.invitePlayer(team.name, target.getUUID());
                        player.sendSystemMessage(Component.literal("已邀请 " + target.getName().getString() + " 加入队伍"));

                        Component acceptBtn = Component.literal("[接受]")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpass team accept " + team.name)));
                        Component denyBtn = Component.literal("[拒绝]")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpass team deny " + team.name)));

                        target.sendSystemMessage(Component.literal("你收到了加入队伍 [" + team.name + "] 的邀请 ")
                                .append(acceptBtn)
                                .append(" ")
                                .append(denyBtn));
                        return 1;
                    })
                )
            )
            .then(Commands.literal("join")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String name = StringArgumentType.getString(context, "name");
                        if (TeamManager.joinTeam(name, player)) {
                            player.sendSystemMessage(Component.literal("成功加入队伍 [" + name + "]"));
                            for (UUID member : TeamManager.getTeam(name).getMembers()) {
                                ServerPlayer memberPlayer = player.getServer().getPlayerList().getPlayer(member);
                                if (memberPlayer != null && !memberPlayer.equals(player)) {
                                    memberPlayer.sendSystemMessage(Component.literal(player.getName().getString() + " 加入了队伍"));
                                }
                            }
                        } else {
                            player.sendSystemMessage(Component.literal("加入失败：没有收到邀请或你已在其他队伍中"));
                        }
                        return 1;
                    })
                )
            )
            .then(Commands.literal("accept")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String name = StringArgumentType.getString(context, "name");
                        if (TeamManager.joinTeam(name, player)) {
                            player.sendSystemMessage(Component.literal("成功加入队伍 [" + name + "]"));
                            for (UUID member : TeamManager.getTeam(name).getMembers()) {
                                ServerPlayer memberPlayer = player.getServer().getPlayerList().getPlayer(member);
                                if (memberPlayer != null && !memberPlayer.equals(player)) {
                                    memberPlayer.sendSystemMessage(Component.literal(player.getName().getString() + " 加入了队伍"));
                                }
                            }
                        } else {
                            player.sendSystemMessage(Component.literal("加入失败：没有收到邀请或你已在其他队伍中"));
                        }
                        return 1;
                    })
                )
            )
            .then(Commands.literal("deny")
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String name = StringArgumentType.getString(context, "name");
                        if (TeamManager.denyInvite(name, player.getUUID())) {
                            player.sendSystemMessage(Component.literal("已拒绝加入队伍 [" + name + "]"));
                        } else {
                            player.sendSystemMessage(Component.literal("拒绝失败：没有收到该队伍的邀请"));
                        }
                        return 1;
                    })
                )
            )
            .then(Commands.literal("leave")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TeamData team = TeamManager.getPlayerTeam(player);
                    if (team == null) {
                        player.sendSystemMessage(Component.literal("你不在任何队伍中"));
                        return 0;
                    }
                    String teamName = team.name;
                    if (TeamManager.leaveTeam(player)) {
                        player.sendSystemMessage(Component.literal("你已离开队伍 [" + teamName + "]"));
                    } else {
                        player.sendSystemMessage(Component.literal("离开失败"));
                    }
                    return 1;
                })
            )
            .then(Commands.literal("kick")
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        ServerPlayer target = EntityArgument.getPlayer(context, "target");
                        TeamData team = TeamManager.getPlayerTeam(player);
                        if (team == null) {
                            player.sendSystemMessage(Component.literal("你不在任何队伍中"));
                            return 0;
                        }
                        if (!team.leader.equals(player.getUUID())) {
                            player.sendSystemMessage(Component.literal("只有队长可以踢人"));
                            return 0;
                        }
                        if (target.equals(player)) {
                            player.sendSystemMessage(Component.literal("不能踢出自己"));
                            return 0;
                        }
                        if (TeamManager.kickPlayer(team.name, target.getUUID(), player)) {
                            player.sendSystemMessage(Component.literal("已将 " + target.getName().getString() + " 踢出队伍"));
                            target.sendSystemMessage(Component.literal("你已被踢出队伍 [" + team.name + "]"));
                        } else {
                            player.sendSystemMessage(Component.literal("踢出失败"));
                        }
                        return 1;
                    })
                )
            )
            .then(Commands.literal("disband")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TeamData team = TeamManager.getPlayerTeam(player);
                    if (team == null) {
                        player.sendSystemMessage(Component.literal("你不在任何队伍中"));
                        return 0;
                    }
                    if (!team.leader.equals(player.getUUID())) {
                        player.sendSystemMessage(Component.literal("只有队长可以解散队伍"));
                        return 0;
                    }
                    TeamManager.removeTeamChest(team.name);
                    if (TeamManager.disbandTeam(team.name, player)) {
                        player.sendSystemMessage(Component.literal("队伍 [" + team.name + "] 已解散"));
                    } else {
                        player.sendSystemMessage(Component.literal("解散失败"));
                    }
                    return 1;
                })
            )
            .then(Commands.literal("list")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    java.util.Map<String, TeamData> allTeams = TeamManager.getAllTeams();
                    if (allTeams.isEmpty()) {
                        player.sendSystemMessage(Component.literal("当前没有队伍"));
                        return 1;
                    }
                    player.sendSystemMessage(Component.literal("=== 队伍列表 ==="));
                    for (java.util.Map.Entry<String, TeamData> entry : allTeams.entrySet()) {
                        player.sendSystemMessage(Component.literal("[" + entry.getKey() + "] 人数: " + entry.getValue().getSize()));
                    }
                    return 1;
                })
            )
            .then(Commands.literal("info")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TeamData team = TeamManager.getPlayerTeam(player);
                    if (team == null) {
                        player.sendSystemMessage(Component.literal("你不在任何队伍中"));
                        return 0;
                    }
                    player.sendSystemMessage(Component.literal("=== 队伍信息 ==="));
                    player.sendSystemMessage(Component.literal("队伍名: " + team.name));
                    player.sendSystemMessage(Component.literal("人数: " + team.getSize()));
                    player.sendSystemMessage(Component.literal("PVP: " + (team.isPvpEnabled() ? "开启" : "关闭")));
                    player.sendSystemMessage(Component.literal("队长: " + (player.getServer().getPlayerList().getPlayer(team.leader) != null ? player.getServer().getPlayerList().getPlayer(team.leader).getName().getString() : team.leader.toString())));
                    player.sendSystemMessage(Component.literal("队员:"));
                    for (UUID memberId : team.getMembers()) {
                        ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
                        String name = member != null ? member.getName().getString() : memberId.toString();
                        boolean isLeader = memberId.equals(team.leader);
                        player.sendSystemMessage(Component.literal("  - " + name + (isLeader ? " (队长)" : "")));
                    }
                    return 1;
                })
            )
            .then(Commands.literal("pvp")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TeamData team = TeamManager.getPlayerTeam(player);
                    if (team == null) {
                        player.sendSystemMessage(Component.literal("你不在任何队伍中"));
                        return 0;
                    }
                    if (!team.leader.equals(player.getUUID())) {
                        player.sendSystemMessage(Component.literal("只有队长可以设置PVP"));
                        return 0;
                    }
                    boolean current = team.isPvpEnabled();
                    Component onBtn = Component.literal("[开启]")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpass team pvp true")));
                    Component offBtn = Component.literal("[关闭]")
                            .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpass team pvp false")));
                    player.sendSystemMessage(Component.literal("当前PVP状态: " + (current ? "开启" : "关闭") + " ").append(onBtn).append(" ").append(offBtn));
                    return 1;
                })
                .then(Commands.argument("value", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String value = StringArgumentType.getString(context, "value");
                        TeamData team = TeamManager.getPlayerTeam(player);
                        if (team == null) {
                            player.sendSystemMessage(Component.literal("你不在任何队伍中"));
                            return 0;
                        }
                        if (!team.leader.equals(player.getUUID())) {
                            player.sendSystemMessage(Component.literal("只有队长可以设置PVP"));
                            return 0;
                        }
                        if (value.equalsIgnoreCase("true")) {
                            TeamManager.setPvpEnabled(team.name, true);
                            player.sendSystemMessage(Component.literal("队伍PVP已开启"));
                        } else if (value.equalsIgnoreCase("false")) {
                            TeamManager.setPvpEnabled(team.name, false);
                            player.sendSystemMessage(Component.literal("队伍PVP已关闭"));
                        } else {
                            player.sendSystemMessage(Component.literal("参数错误，请输入 true 或 false"));
                        }
                        return 1;
                    })
                )
            )
            .then(Commands.literal("repository")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TeamData team = TeamManager.getPlayerTeam(player);
                    if (team == null) {
                        player.sendSystemMessage(Component.literal("你不在任何队伍中"));
                        return 0;
                    }
                    net.minecraft.world.SimpleContainer container = TeamManager.getTeamChest(team.name);
                    player.openMenu(new SimpleMenuProvider((id, inv, p) ->
                            ChestMenu.threeRows(id, inv, container),
                            Component.literal("队伍共享末影箱 - " + team.name)));
                    return 1;
                })
            )
            .then(Commands.literal("r")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TeamData team = TeamManager.getPlayerTeam(player);
                    if (team == null) {
                        player.sendSystemMessage(Component.literal("你不在任何队伍中"));
                        return 0;
                    }
                    net.minecraft.world.SimpleContainer container = TeamManager.getTeamChest(team.name);
                    player.openMenu(new SimpleMenuProvider((id, inv, p) ->
                            ChestMenu.threeRows(id, inv, container),
                            Component.literal("队伍共享末影箱 - " + team.name)));
                    return 1;
                })
            )
            .then(Commands.literal("color")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ChatFormatting[] colors = {
                        ChatFormatting.BLACK, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_GREEN,
                        ChatFormatting.DARK_AQUA, ChatFormatting.DARK_RED, ChatFormatting.DARK_PURPLE,
                        ChatFormatting.GOLD, ChatFormatting.GRAY, ChatFormatting.DARK_GRAY,
                        ChatFormatting.BLUE, ChatFormatting.GREEN, ChatFormatting.AQUA,
                        ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE, ChatFormatting.YELLOW,
                        ChatFormatting.WHITE
                    };
                    net.minecraft.network.chat.MutableComponent message = Component.literal("选择发光颜色：");
                    for (ChatFormatting color : colors) {
                        Component colorBtn = Component.literal("■")
                                .withStyle(Style.EMPTY.withColor(color).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpass team color " + color.name())));
                        message = message.append(Component.literal(" ")).append(colorBtn);
                    }
                    player.sendSystemMessage(message);
                    return 1;
                })
                .then(Commands.argument("color", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String colorName = StringArgumentType.getString(context, "color");
                        ChatFormatting color;
                        try {
                            color = ChatFormatting.valueOf(colorName.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            player.sendSystemMessage(Component.literal("无效的颜色"));
                            return 0;
                        }
                        if (!color.isColor()) {
                            player.sendSystemMessage(Component.literal("无效的颜色"));
                            return 0;
                        }
                        TeamManager.setGlowColor(player.getUUID(), color);
                        player.sendSystemMessage(Component.literal("发光颜色已设置为 ").append(Component.literal("■").withStyle(Style.EMPTY.withColor(color))));
                        return 1;
                    })
                )
            );
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // 移除原版 /team 命令，使用我们的
        try {
            java.lang.reflect.Field childrenField = com.mojang.brigadier.tree.CommandNode.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            java.util.Map<String, ?> children = (java.util.Map<String, ?>) childrenField.get(event.getDispatcher().getRoot());
            children.remove("team");
        } catch (Exception e) {
            e.printStackTrace();
        }

        event.getDispatcher().register(
            Commands.literal("tpa")
                .requires(source -> source.getPlayer() != null)
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer requester = context.getSource().getPlayerOrException();
                        ServerPlayer target = EntityArgument.getPlayer(context, "target");

                        if (requester.equals(target)) {
                            requester.sendSystemMessage(Component.literal("你不能向自己发送传送请求"));
                            return 0;
                        }

                        TeleportManager.sendRequest(requester, target);
                        return 1;
                    })
                )
        );

        event.getDispatcher().register(
            Commands.literal("tpaccept")
                .requires(source -> source.getPlayer() != null)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TeleportManager.acceptRequest(player);
                    return 1;
                })
        );

        event.getDispatcher().register(
            Commands.literal("js")
                .requires(source -> source.getPlayer() != null)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TeleportManager.acceptRequest(player);
                    return 1;
                })
        );

        event.getDispatcher().register(
            Commands.literal("tpdeny")
                .requires(source -> source.getPlayer() != null)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TeleportManager.denyRequest(player);
                    return 1;
                })
        );

        event.getDispatcher().register(
            Commands.literal("back")
                .requires(source -> source.getPlayer() != null)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TeleportManager.back(player);
                    return 1;
                })
        );

        event.getDispatcher().register(
            Commands.literal("sethome")
                .requires(source -> source.getPlayer() != null)
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String name = StringArgumentType.getString(context, "name");
                        TeleportManager.setHome(player, name);
                        return 1;
                    })
                )
        );

        event.getDispatcher().register(
            Commands.literal("home")
                .requires(source -> source.getPlayer() != null)
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String name = StringArgumentType.getString(context, "name");
                        TeleportManager.goHome(player, name);
                        return 1;
                    })
                )
        );

        event.getDispatcher().register(
            Commands.literal("rehome")
                .requires(source -> source.getPlayer() != null)
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String name = StringArgumentType.getString(context, "name");
                        TeleportManager.removeHome(player, name);
                        return 1;
                    })
                )
        );

        event.getDispatcher().register(
            Commands.literal("setwarp")
                .requires(source -> source.getPlayer() != null)
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String name = StringArgumentType.getString(context, "name");
                        TeleportManager.setTeamWarp(player, name);
                        return 1;
                    })
                )
        );

        event.getDispatcher().register(
            Commands.literal("warp")
                .requires(source -> source.getPlayer() != null)
                .then(Commands.argument("name", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String name = StringArgumentType.getString(context, "name");
                        TeleportManager.goTeamWarp(player, name);
                        return 1;
                    })
                )
        );

        event.getDispatcher().register(
            Commands.literal("warps")
                .requires(source -> source.getPlayer() != null)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TeleportManager.listTeamWarps(player);
                    return 1;
                })
        );

        event.getDispatcher().register(
            Commands.literal("tpass")
                .then(Commands.literal("help")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        player.sendSystemMessage(Component.literal("=== TPass 模组帮助 ==="));
                        player.sendSystemMessage(Component.literal("/tpa <玩家名> - 向玩家发送传送请求"));
                        player.sendSystemMessage(Component.literal("/tpaccept - 接受传送请求"));
                        player.sendSystemMessage(Component.literal("/tpdeny - 拒绝传送请求"));
                        player.sendSystemMessage(Component.literal("/js - 接受传送请求（快捷方式）"));
                        player.sendSystemMessage(Component.literal("/back - 返回上一个位置/死亡点"));
                        player.sendSystemMessage(Component.literal("/sethome <名称> - 设置传送点（最多5个）"));
                        player.sendSystemMessage(Component.literal("/home <名称> - 传送到指定传送点"));
                        player.sendSystemMessage(Component.literal("/rehome <名称> - 删除指定传送点"));
                        player.sendSystemMessage(Component.literal("/setwarp <名称> - 设置队伍传送点"));
                        player.sendSystemMessage(Component.literal("/warp <名称> - 传送到队伍传送点"));
                        player.sendSystemMessage(Component.literal("/warps - 查看队伍传送点列表"));
                        player.sendSystemMessage(Component.literal("/tpass team create <队伍名> - 创建队伍"));
                        player.sendSystemMessage(Component.literal("/tpass team invite <玩家名> - 邀请玩家加入队伍"));
                        player.sendSystemMessage(Component.literal("/tpass team join <队伍名> - 接受邀请加入队伍"));
                        player.sendSystemMessage(Component.literal("/tpass team leave - 离开队伍"));
                        player.sendSystemMessage(Component.literal("/tpass team kick <玩家名> - 踢出队员"));
                        player.sendSystemMessage(Component.literal("/tpass team disband - 解散队伍"));
                        player.sendSystemMessage(Component.literal("/tpass team color - 设置发光颜色"));
                        player.sendSystemMessage(Component.literal("/tpass team list - 查看所有队伍"));
                        player.sendSystemMessage(Component.literal("/tpass team info - 查看队伍信息"));
                        player.sendSystemMessage(Component.literal("/tpass team pvp - 设置队伍PVP"));
                        player.sendSystemMessage(Component.literal("/tpass team repository - 打开队伍共享末影箱"));
                        player.sendSystemMessage(Component.literal("/flightcard - 使用创造飞行体验卡（飞行5分钟，可暂停，冷却20分钟）"));
                        return 1;
                    })
                )
        );

        event.getDispatcher().register(
            Commands.literal("tpass")
                .then(buildTeamCommands())
        );

        event.getDispatcher().register(
            Commands.literal("flightcard")
                .requires(source -> source.getPlayer() != null)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    FlightCardManager.useFlightCard(player);
                    return 1;
                })
        );

        event.getDispatcher().register(
                buildTeamCommands()
                        .requires(source -> source.getPlayer() != null)
        );

        event.getDispatcher().register(
            Commands.literal("tt")
                .requires(source -> source.getPlayer() != null)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TeamData team = TeamManager.getPlayerTeam(player);
                    if (team == null) {
                        Component createBtn = Component.literal("[创建队伍]")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tpass team create ")));
                        Component joinBtn = Component.literal("[申请加入队伍]")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tt joinlist")));

                        player.sendSystemMessage(Component.literal("=== 队伍菜单 ==="));
                        player.sendSystemMessage(createBtn);
                        player.sendSystemMessage(joinBtn);
                        player.sendSystemMessage(Component.literal("=================="));
                    } else {
                        String leaderName = player.getServer().getPlayerList().getPlayer(team.leader) != null
                                ? player.getServer().getPlayerList().getPlayer(team.leader).getName().getString()
                                : team.leader.toString();

                        player.sendSystemMessage(Component.literal("=== 队伍菜单 ==="));
                        player.sendSystemMessage(Component.literal("队伍名: " + team.name));
                        player.sendSystemMessage(Component.literal("队长: " + leaderName));
                        player.sendSystemMessage(Component.literal("PVP: " + (team.isPvpEnabled() ? "开启" : "关闭")));
                        player.sendSystemMessage(Component.literal("队员:"));
                        for (UUID memberId : team.getMembers()) {
                            ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
                            String memberName = member != null ? member.getName().getString() : memberId.toString();
                            if (member != null && !memberId.equals(player.getUUID())) {
                                Component memberBtn = Component.literal("  - " + memberName)
                                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpa " + memberName)));
                                player.sendSystemMessage(memberBtn);
                            } else {
                                Component memberGray = Component.literal("  - " + memberName)
                                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
                                player.sendSystemMessage(memberGray);
                            }
                        }

                        Component inviteBtn = Component.literal("[邀请玩家]")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tpass team invite ")));
                        boolean pvpEnabled = team.isPvpEnabled();
                        ChatFormatting pvpColor = pvpEnabled ? ChatFormatting.RED : ChatFormatting.GREEN;
                        String pvpCmd = pvpEnabled ? "/tpass team pvp false" : "/tpass team pvp true";
                        String pvpText = pvpEnabled ? "[pvp开启]" : "[pvp关闭]";
                        Component pvpBtn = Component.literal(pvpText)
                                .withStyle(Style.EMPTY.withColor(pvpColor).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, pvpCmd)));
                        Component repoBtn = Component.literal("[队伍仓库]")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpass team repository")));
                        player.sendSystemMessage(inviteBtn.copy().append(Component.literal(" ")).append(pvpBtn).append(Component.literal(" ")).append(repoBtn));

                        Component leaveBtn = Component.literal("[离开队伍]")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tt leaveconfirm")));
                        Component disbandBtn = Component.literal("[解散队伍]")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tt disbandconfirm")));
                        player.sendSystemMessage(leaveBtn.copy().append(Component.literal(" ")).append(disbandBtn));

                        Component warpBtn = Component.literal("[队伍传送点]")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tt warplist")));
                        player.sendSystemMessage(warpBtn);

                        player.sendSystemMessage(Component.literal("=================="));

                        player.sendSystemMessage(Component.literal("发光颜色:"));
                        net.minecraft.network.chat.MutableComponent colorLine = Component.literal("");
                        ChatFormatting[] colors = {
                            ChatFormatting.BLACK, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_GREEN,
                            ChatFormatting.DARK_AQUA, ChatFormatting.DARK_RED, ChatFormatting.DARK_PURPLE,
                            ChatFormatting.GOLD, ChatFormatting.GRAY, ChatFormatting.DARK_GRAY,
                            ChatFormatting.BLUE, ChatFormatting.GREEN, ChatFormatting.AQUA,
                            ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE, ChatFormatting.YELLOW,
                            ChatFormatting.WHITE
                        };
                        for (ChatFormatting color : colors) {
                            Component colorBlock = Component.literal("■")
                                    .withStyle(Style.EMPTY.withColor(color).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpass team color " + color.name())));
                            colorLine = colorLine.append(colorBlock).append(Component.literal(" "));
                        }
                        player.sendSystemMessage(colorLine);
                    }
                    return 1;
                })
                .then(Commands.literal("joinlist")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        player.sendSystemMessage(Component.literal("=== 队伍菜单 ==="));
                        player.sendSystemMessage(Component.literal("请选择要加入的队伍:"));
                        java.util.Map<String, TeamData> allTeams = TeamManager.getAllTeams();
                        if (allTeams.isEmpty()) {
                            player.sendSystemMessage(Component.literal("当前没有队伍"));
                        } else {
                            for (java.util.Map.Entry<String, TeamData> entry : allTeams.entrySet()) {
                                Component teamBtn = Component.literal("[" + entry.getKey() + "]")
                                        .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpass team join " + entry.getKey())));
                                player.sendSystemMessage(teamBtn);
                            }
                        }
                        player.sendSystemMessage(Component.literal("=================="));
                        return 1;
                    })
                )
                .then(Commands.literal("leaveconfirm")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        TeamData team = TeamManager.getPlayerTeam(player);
                        if (team == null) {
                            player.sendSystemMessage(Component.literal("你不在任何队伍中"));
                            return 0;
                        }
                        Component confirmBtn = Component.literal("[确认离开]")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpass team leave")));
                        Component cancelBtn = Component.literal("[取消]")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tt")));
                        player.sendSystemMessage(Component.literal("确定要离开队伍吗？"));
                        player.sendSystemMessage(confirmBtn.copy().append(Component.literal(" ")).append(cancelBtn));
                        return 1;
                    })
                )
                .then(Commands.literal("disbandconfirm")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        TeamData team = TeamManager.getPlayerTeam(player);
                        if (team == null) {
                            player.sendSystemMessage(Component.literal("你不在任何队伍中"));
                            return 0;
                        }
                        if (!team.leader.equals(player.getUUID())) {
                            player.sendSystemMessage(Component.literal("只有队长可以解散队伍"));
                            return 0;
                        }
                        Component confirmBtn = Component.literal("[确认解散]")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpass team disband")));
                        Component cancelBtn = Component.literal("[取消]")
                                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tt")));
                        player.sendSystemMessage(Component.literal("确定要解散队伍吗？"));
                        player.sendSystemMessage(confirmBtn.copy().append(Component.literal(" ")).append(cancelBtn));
                        return 1;
                    })
                )
                .then(Commands.literal("warplist")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        TeamData team = TeamManager.getPlayerTeam(player);
                        if (team == null) {
                            player.sendSystemMessage(Component.literal("你不在任何队伍中"));
                            return 0;
                        }
                        player.sendSystemMessage(Component.literal("=== warps ==="));
                        var warps = team.getWarps();
                        if (warps.isEmpty()) {
                            player.sendSystemMessage(Component.literal("当前队伍没有设置任何传送点"));
                        } else {
                            for (String warpName : warps.keySet()) {
                                Component wBtn = Component.literal("[ " + warpName + " ]")
                                        .withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp " + warpName)));
                                player.sendSystemMessage(wBtn);
                            }
                        }
                        player.sendSystemMessage(Component.literal("=================="));
                        return 1;
                    })
                )
        );
    }

    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event) {
        TeamStorage.load(Paths.get("."));
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        TeamStorage.save(Paths.get("."));
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        event.getEntity().sendSystemMessage(Component.literal("感谢使用tpss模组"));
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 玩家退出时清除飞行状态（不发送消息）
            FlightCardManager.disableFlight(player, false);
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TeleportManager.saveLocation(player);
        }
    }

    private int tickCounter = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        // 更新飞行卡时间（每20 tick = 1秒）
        tickCounter++;
        if (tickCounter >= 20) {
            tickCounter = 0;
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                FlightCardManager.updateFlightTime(player);
            }
        }
        
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            TeamData team = TeamManager.getPlayerTeam(player);
            Scoreboard scoreboard = player.getServer().getLevel(Level.OVERWORLD).getScoreboard();
            String glowTeamName = "tpass_glow_" + player.getUUID();

            if (team == null) {
                PlayerTeam glowTeam = scoreboard.getPlayerTeam(player.getName().getString());
                if (glowTeam != null && glowTeam.getName().startsWith("tpass_glow_")) {
                    scoreboard.removePlayerFromTeam(player.getName().getString(), glowTeam);
                }
                continue;
            }

            PlayerTeam glowTeam = scoreboard.getPlayerTeam(player.getName().getString());
            if (glowTeam == null || !glowTeam.getName().equals(glowTeamName)) {
                if (glowTeam != null) {
                    scoreboard.removePlayerFromTeam(player.getName().getString(), glowTeam);
                }
                glowTeam = scoreboard.addPlayerTeam(glowTeamName);
            }
            ChatFormatting color = TeamManager.getGlowColor(player.getUUID());
            glowTeam.setColor(color);
            scoreboard.addPlayerToTeam(player.getName().getString(), glowTeam);

            for (UUID memberId : team.getMembers()) {
                if (memberId.equals(player.getUUID())) continue;
                ServerPlayer member = event.getServer().getPlayerList().getPlayer(memberId);
                if (member != null) {
                    member.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING, 40, 0, false, false, false));
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        
        TeamData victimTeam = TeamManager.getPlayerTeam(victim);
        TeamData attackerTeam = TeamManager.getPlayerTeam(attacker);
        
        if (victimTeam == null || attackerTeam == null) return;
        if (!victimTeam.name.equals(attackerTeam.name)) return;
        
        if (!victimTeam.isPvpEnabled()) {
            event.setCanceled(true);
        }
    }
}
