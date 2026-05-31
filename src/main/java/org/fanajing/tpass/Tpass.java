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
import org.fanajing.tpass.adapter.VersionAdapter;
import org.fanajing.tpass.adapter.VersionAdapterFactory;
import org.fanajing.tpass.team.TeamData;
import org.fanajing.tpass.team.TeamManager;
import org.fanajing.tpass.team.TeamStorage;
import org.fanajing.tpass.teleport.TeleportManager;
import org.fanajing.tpass.FlightCardManager;

import java.nio.file.Paths;
import java.util.UUID;

@Mod("tpass")
public class Tpass {
    
    private static final VersionAdapter adapter = VersionAdapterFactory.getAdapter();

    public Tpass() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> buildTeamCommands() {
        return Commands.literal("team")
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.string())
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

                        Component acceptBtn = adapter.styleComponent(
                                adapter.createTextComponent("[接受]"),
                                ChatFormatting.GREEN,
                                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpass team accept " + team.name));
                        Component denyBtn = adapter.styleComponent(
                                adapter.createTextComponent("[拒绝]"),
                                ChatFormatting.RED,
                                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpass team deny " + team.name));

                        adapter.sendSystemMessage(target, adapter.appendComponents(
                                adapter.createTextComponent("你收到了加入队伍 [" + team.name + "] 的邀请 "),
                                acceptBtn,
                                adapter.createTextComponent(" "),
                                denyBtn));
                        return 1;
                    })
                )
            )
            .then(Commands.literal("join")
                .then(Commands.argument("name", StringArgumentType.string())
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
                .then(Commands.argument("name", StringArgumentType.string())
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
                .then(Commands.argument("name", StringArgumentType.string())
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
                    Component onBtn = adapter.styleComponent(
                            adapter.createTextComponent("[开启]"),
                            ChatFormatting.GREEN,
                            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpass team pvp true"));
                    Component offBtn = adapter.styleComponent(
                            adapter.createTextComponent("[关闭]"),
                            ChatFormatting.RED,
                            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpass team pvp false"));
                    adapter.sendSystemMessage(player, adapter.appendComponents(
                            adapter.createTextComponent("当前PVP状态: " + (current ? "开启" : "关闭") + " "),
                            onBtn,
                            adapter.createTextComponent(" "),
                            offBtn));
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
                    adapter.openChestGui(player, adapter.createTextComponent("队伍共享末影箱 - " + team.name), container, 3);
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
                    Component message = adapter.createTextComponent("选择发光颜色：");
                    for (ChatFormatting color : colors) {
                        Component colorBtn = adapter.styleComponent(
                                adapter.createTextComponent("■"),
                                color,
                                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpass team color " + color.name()));
                        message = message.copy().append(adapter.createTextComponent(" ")).append(colorBtn);
                    }
                    adapter.sendSystemMessage(player, message);
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
                        adapter.sendSystemMessage(player, adapter.appendComponents(
                                adapter.createTextComponent("发光颜色已设置为 "),
                                adapter.styleComponent(adapter.createTextComponent("■"), color, null)));
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
            Commands.literal("homes")
                .requires(source -> source.getPlayer() != null)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    TeleportManager.listHomes(player);
                    return 1;
                })
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
                        player.sendSystemMessage(Component.literal("/homes - 查看所有传送点列表"));
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
                        Component createBtn = adapter.createSuggestCommandButton("[创建队伍]", ChatFormatting.GREEN, "/tpass team create ");
                        Component joinBtn = adapter.createButtonComponent("[申请加入队伍]", ChatFormatting.AQUA, "/tt joinlist");

                        adapter.sendSystemMessage(player, adapter.createTextComponent("=== 队伍菜单 ==="));
                        adapter.sendSystemMessage(player, createBtn);
                        adapter.sendSystemMessage(player, joinBtn);
                        adapter.sendSystemMessage(player, adapter.createTextComponent("=================="));
                    } else {
                        String leaderName = player.getServer().getPlayerList().getPlayer(team.leader) != null
                                ? player.getServer().getPlayerList().getPlayer(team.leader).getName().getString()
                                : team.leader.toString();

                        adapter.sendSystemMessage(player, adapter.createTextComponent("=== 队伍菜单 ==="));
                        adapter.sendSystemMessage(player, adapter.createTextComponent("队伍名: " + team.name));
                        adapter.sendSystemMessage(player, adapter.createTextComponent("队长: " + leaderName));
                        adapter.sendSystemMessage(player, adapter.createTextComponent("PVP: " + (team.isPvpEnabled() ? "开启" : "关闭")));
                        adapter.sendSystemMessage(player, adapter.createTextComponent("队员:"));
                        for (UUID memberId : team.getMembers()) {
                            ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
                            String memberName = member != null ? member.getName().getString() : memberId.toString();
                            if (member != null && !memberId.equals(player.getUUID())) {
                                Component memberBtn = adapter.createButtonComponent("  - " + memberName, ChatFormatting.GREEN, "/tpa " + memberName);
                                adapter.sendSystemMessage(player, memberBtn);
                            } else {
                                Component memberGray = adapter.styleComponent(adapter.createTextComponent("  - " + memberName), ChatFormatting.GRAY, null);
                                adapter.sendSystemMessage(player, memberGray);
                            }
                        }

                        Component inviteBtn = adapter.createSuggestCommandButton("[邀请玩家]", ChatFormatting.GREEN, "/tpass team invite ");
                        boolean pvpEnabled = team.isPvpEnabled();
                        ChatFormatting pvpColor = pvpEnabled ? ChatFormatting.RED : ChatFormatting.GREEN;
                        String pvpCmd = pvpEnabled ? "/tpass team pvp false" : "/tpass team pvp true";
                        String pvpText = pvpEnabled ? "[pvp开启]" : "[pvp关闭]";
                        Component pvpBtn = adapter.createButtonComponent(pvpText, pvpColor, pvpCmd);
                        Component repoBtn = adapter.createButtonComponent("[队伍仓库]", ChatFormatting.GOLD, "/tpass team repository");
                        adapter.sendSystemMessage(player, adapter.appendComponents(inviteBtn, adapter.createTextComponent(" "), pvpBtn, adapter.createTextComponent(" "), repoBtn));

                        Component leaveBtn = adapter.createButtonComponent("[离开队伍]", ChatFormatting.RED, "/tt leaveconfirm");
                        Component disbandBtn = adapter.createButtonComponent("[解散队伍]", ChatFormatting.DARK_RED, "/tt disbandconfirm");
                        adapter.sendSystemMessage(player, adapter.appendComponents(leaveBtn, adapter.createTextComponent(" "), disbandBtn));

                        Component warpBtn = adapter.createButtonComponent("[队伍传送点]", ChatFormatting.LIGHT_PURPLE, "/tt warplist");
                        adapter.sendSystemMessage(player, warpBtn);

                        adapter.sendSystemMessage(player, adapter.createTextComponent("=================="));

                        adapter.sendSystemMessage(player, adapter.createTextComponent("发光颜色:"));
                        ChatFormatting[] colors = {
                            ChatFormatting.BLACK, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_GREEN,
                            ChatFormatting.DARK_AQUA, ChatFormatting.DARK_RED, ChatFormatting.DARK_PURPLE,
                            ChatFormatting.GOLD, ChatFormatting.GRAY, ChatFormatting.DARK_GRAY,
                            ChatFormatting.BLUE, ChatFormatting.GREEN, ChatFormatting.AQUA,
                            ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE, ChatFormatting.YELLOW,
                            ChatFormatting.WHITE
                        };
                        Component colorLine = adapter.createColorPicker(colors, "/tpass team color ");
                        adapter.sendSystemMessage(player, colorLine);
                    }
                    return 1;
                })
                .then(Commands.literal("joinlist")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        adapter.sendSystemMessage(player, adapter.createTextComponent("=== 队伍菜单 ==="));
                        adapter.sendSystemMessage(player, adapter.createTextComponent("请选择要加入的队伍:"));
                        java.util.Map<String, TeamData> allTeams = TeamManager.getAllTeams();
                        if (allTeams.isEmpty()) {
                            adapter.sendSystemMessage(player, adapter.createTextComponent("当前没有队伍"));
                        } else {
                            for (java.util.Map.Entry<String, TeamData> entry : allTeams.entrySet()) {
                                Component teamBtn = adapter.createButtonComponent("[" + entry.getKey() + "]", ChatFormatting.AQUA, "/tpass team join " + entry.getKey());
                                adapter.sendSystemMessage(player, teamBtn);
                            }
                        }
                        adapter.sendSystemMessage(player, adapter.createTextComponent("=================="));
                        return 1;
                    })
                )
                .then(Commands.literal("leaveconfirm")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        TeamData team = TeamManager.getPlayerTeam(player);
                        if (team == null) {
                            adapter.sendSystemMessage(player, adapter.createTextComponent("你不在任何队伍中"));
                            return 0;
                        }
                        Component confirmBtn = adapter.createButtonComponent("[确认离开]", ChatFormatting.RED, "/tpass team leave");
                        Component cancelBtn = adapter.createButtonComponent("[取消]", ChatFormatting.GREEN, "/tt");
                        adapter.sendSystemMessage(player, adapter.createTextComponent("确定要离开队伍吗？"));
                        adapter.sendSystemMessage(player, adapter.appendComponents(confirmBtn, adapter.createTextComponent(" "), cancelBtn));
                        return 1;
                    })
                )
                .then(Commands.literal("disbandconfirm")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        TeamData team = TeamManager.getPlayerTeam(player);
                        if (team == null) {
                            adapter.sendSystemMessage(player, adapter.createTextComponent("你不在任何队伍中"));
                            return 0;
                        }
                        if (!team.leader.equals(player.getUUID())) {
                            adapter.sendSystemMessage(player, adapter.createTextComponent("只有队长可以解散队伍"));
                            return 0;
                        }
                        Component confirmBtn = adapter.createButtonComponent("[确认解散]", ChatFormatting.RED, "/tpass team disband");
                        Component cancelBtn = adapter.createButtonComponent("[取消]", ChatFormatting.GREEN, "/tt");
                        adapter.sendSystemMessage(player, adapter.createTextComponent("确定要解散队伍吗？"));
                        adapter.sendSystemMessage(player, adapter.appendComponents(confirmBtn, adapter.createTextComponent(" "), cancelBtn));
                        return 1;
                    })
                )
                .then(Commands.literal("warplist")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        TeamData team = TeamManager.getPlayerTeam(player);
                        if (team == null) {
                            adapter.sendSystemMessage(player, adapter.createTextComponent("你不在任何队伍中"));
                            return 0;
                        }
                        adapter.sendSystemMessage(player, adapter.createTextComponent("=== warps ==="));
                        var warps = team.getWarps();
                        if (warps.isEmpty()) {
                            adapter.sendSystemMessage(player, adapter.createTextComponent("当前队伍没有设置任何传送点"));
                        } else {
                            for (String warpName : warps.keySet()) {
                                Component wBtn = adapter.createButtonComponent("[ " + warpName + " ]", ChatFormatting.AQUA, "/warp " + warpName);
                                adapter.sendSystemMessage(player, wBtn);
                            }
                        }
                        adapter.sendSystemMessage(player, adapter.createTextComponent("=================="));
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
            String glowTeamName = "tpass_glow_" + adapter.getPlayerUUID(player);

            if (team == null) {
                adapter.removePlayerFromGlowTeam(scoreboard, adapter.getPlayerName(player).getString(), glowTeamName);
                continue;
            }

            ChatFormatting color = TeamManager.getGlowColor(adapter.getPlayerUUID(player));
            adapter.setupGlowTeam(scoreboard, player, glowTeamName, color);

            for (UUID memberId : team.getMembers()) {
                if (memberId.equals(adapter.getPlayerUUID(player))) continue;
                ServerPlayer member = event.getServer().getPlayerList().getPlayer(memberId);
                if (member != null) {
                    adapter.addGlowingEffect(member, 40);
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
