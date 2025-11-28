package com.ausganslage.ausgangslageBackend.config;

import com.ausganslage.ausgangslageBackend.enums.Faction;
import com.ausganslage.ausgangslageBackend.enums.RoleName;
import com.ausganslage.ausgangslageBackend.model.RoleTemplate;
import com.ausganslage.ausgangslageBackend.repository.RoleTemplateRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final RoleTemplateRepository roleTemplateRepository;

    public DataLoader(RoleTemplateRepository roleTemplateRepository) {
        this.roleTemplateRepository = roleTemplateRepository;
    }

    @Override
    public void run(String... args) {
        if (roleTemplateRepository.count() > 0) {
            System.out.println("Role templates already loaded ✅");
            return;
        }

        RoleTemplate werewolf = new RoleTemplate();
        werewolf.setName(RoleName.WEREWOLF);
        werewolf.setFaction(Faction.WOLVES);
        werewolf.setHasNightPower(true);
        werewolf.setDefaultCount(2);
        werewolf.setDescription("Wakes at night to kill villagers");
        roleTemplateRepository.save(werewolf);

        RoleTemplate villager = new RoleTemplate();
        villager.setName(RoleName.VILLAGER);
        villager.setFaction(Faction.VILLAGE);
        villager.setHasNightPower(false);
        villager.setDefaultCount(0);
        villager.setDescription("Regular villager with no special powers");
        roleTemplateRepository.save(villager);

        RoleTemplate seer = new RoleTemplate();
        seer.setName(RoleName.SEER);
        seer.setFaction(Faction.VILLAGE);
        seer.setHasNightPower(true);
        seer.setDefaultCount(1);
        seer.setDescription("Can investigate one player each night");
        roleTemplateRepository.save(seer);

        RoleTemplate witch = new RoleTemplate();
        witch.setName(RoleName.WITCH);
        witch.setFaction(Faction.VILLAGE);
        witch.setHasNightPower(true);
        witch.setDefaultCount(1);
        witch.setDescription("Has one heal and one poison potion");
        roleTemplateRepository.save(witch);

        RoleTemplate hunter = new RoleTemplate();
        hunter.setName(RoleName.HUNTER);
        hunter.setFaction(Faction.VILLAGE);
        hunter.setHasNightPower(false);
        hunter.setDefaultCount(1);
        hunter.setDescription("Takes revenge when killed");
        roleTemplateRepository.save(hunter);

        System.out.println("Role templates loaded ✅");
    }
}

