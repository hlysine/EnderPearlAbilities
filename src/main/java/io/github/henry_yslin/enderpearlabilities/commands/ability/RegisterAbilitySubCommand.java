package io.github.henry_yslin.enderpearlabilities.commands.ability;

import io.github.henry_yslin.enderpearlabilities.EnderPearlAbilities;
import io.github.henry_yslin.enderpearlabilities.abilities.Ability;
import io.github.henry_yslin.enderpearlabilities.abilities.AbilityInfo;
import io.github.henry_yslin.enderpearlabilities.abilities.ActivationHand;
import io.github.henry_yslin.enderpearlabilities.commands.SubCommand;
import io.github.henry_yslin.enderpearlabilities.utils.PlayerUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class RegisterAbilitySubCommand extends SubCommand {

    protected RegisterAbilitySubCommand() {
        super("register");
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String subcommand, List<String> args) {
        if (!PlayerUtils.checkPermissionOrError(sender, "ability." + subCommandName)) return true;
        if (!EnderPearlAbilities.getInstance().getLoadedConfig().getBoolean("dynamic")) {
            sender.sendMessage(ChatColor.RED + "This command is only allowed when dynamic abilities are on.");
            return true;
        }

        if (args.size() <= 0) return false;
        if (!(sender instanceof Player)) return false;

        AbilityInfo abilityInfo = null;
        List<AbilityInfo> abilityInfos = EnderPearlAbilities.getInstance().getAbilityInfos();
        for (AbilityInfo info : abilityInfos) {
            if (info.getCodeName().equalsIgnoreCase(args.get(0))) {
                abilityInfo = info;
            }
        }

        if (abilityInfo == null) {
            sender.sendMessage(ChatColor.RED + "Cannot find ability with code name " + args.get(0));
            return true;
        }

        List<Ability<?>> abilities;
        synchronized (abilities = EnderPearlAbilities.getInstance().getAbilities()) {
            for (Ability<?> ability : abilities) {
                if (Objects.equals(ability.getOwnerName(), sender.getName())) {
                    if (ability.getInfo().getCodeName().equalsIgnoreCase(args.get(0))) {
                        sender.sendMessage(ChatColor.RED + "You already have this ability registered");
                        return true;
                    }
                    if (ability.getInfo().getActivation() == abilityInfo.getActivation()) {
                        sender.sendMessage(ChatColor.RED + "Your " + (ability.getInfo().getActivation() == ActivationHand.MainHand ? "main" : "off") + " hand is already occupied by the ability " + ability.getInfo().getCodeName());
                        return true;
                    }
                }
            }
        }

        EnderPearlAbilities.getInstance().addAbility(abilityInfo, sender.getName());

        sender.sendMessage("" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "Registered ability " + abilityInfo.getCodeName());
        return true;
    }
}
