package net.flectone.pulse.config;

public interface ModuleConfig {

    CommandConfig getCommand();
    IntegrationConfig getIntegration();
    MessageConfig getMessage();

    interface CommandConfig {

        SubCommandConfig getAfk();
        SubCommandConfig getBall();
        SubCommandConfig getBan();
        SubCommandConfig getBanlist();
        SubCommandConfig getBroadcast();
        SubCommandConfig getChatcolor();
        SubCommandConfig getChatsetting();
        SubCommandConfig getClearchat();
        SubCommandConfig getClearmail();
        SubCommandConfig getCoin();
        SubCommandConfig getDice();
        SubCommandConfig getDo();
        SubCommandConfig getFlectonepulse();
        SubCommandConfig getGeolocate();
        SubCommandConfig getHelper();
        SubCommandConfig getIgnore();
        SubCommandConfig getIgnorelist();
        SubCommandConfig getKick();
        SubCommandConfig getMail();
        SubCommandConfig getMaintenance();
        SubCommandConfig getMark();
        SubCommandConfig getMe();
        SubCommandConfig getMute();
        SubCommandConfig getMutelist();
        SubCommandConfig getOnline();
        SubCommandConfig getPing();
        SubCommandConfig getPoll();
        SubCommandConfig getReply();
        SubCommandConfig getRockpaperscissors();
        SubCommandConfig getSpy();
        SubCommandConfig getStream();
        SubCommandConfig getSymbol();
        SubCommandConfig getTell();
        SubCommandConfig getTictactoe();
        SubCommandConfig getTranslateto();
        SubCommandConfig getTry();
        SubCommandConfig getUnban();
        SubCommandConfig getUnmute();
        SubCommandConfig getUnwarn();
        SubCommandConfig getWarn();
        SubCommandConfig getWarnlist();

        interface SubCommandConfig {}
    }

    interface IntegrationConfig {

        SubIntegrationConfig getDeepl();
        SubIntegrationConfig getDiscord();
        SubIntegrationConfig getLuckperms();
        SubIntegrationConfig getPlaceholderapi();
        SubIntegrationConfig getPlasmovoice();
        SubIntegrationConfig getSimplevoice();
        SubIntegrationConfig getSkinsrestorer();
        SubIntegrationConfig getSupervanish();
        SubIntegrationConfig getTelegram();
        SubIntegrationConfig getTriton();
        SubIntegrationConfig getTwitch();
        SubIntegrationConfig getVault();
        SubIntegrationConfig getYandex();

        interface SubIntegrationConfig {}
    }

    interface MessageConfig {

        SubMessageConfig getAdvancement();
        SubMessageConfig getAfk();
        SubMessageConfig getAnvil();
        SubMessageConfig getAuto();
        SubMessageConfig getBed();
        SubMessageConfig getBook();
        SubMessageConfig getBrand();
        SubMessageConfig getBubble();
        SubMessageConfig getChat();
        SubMessageConfig getClear();
        SubMessageConfig getDeath();
        SubMessageConfig getDeop();
        SubMessageConfig getEnchant();
        FormatMessageConfig getFormat();
        SubMessageConfig getGamemode();
        SubMessageConfig getGreeting();
        SubMessageConfig getJoin();
        SubMessageConfig getMark();
        ObjectiveMessageConfig getObjective();
        SubMessageConfig getOp();
        SubMessageConfig getQuit();
        SubMessageConfig getRightclick();
        SubMessageConfig getScoreboard();
        SubMessageConfig getSeed();
        SubMessageConfig getSetblock();
        SubMessageConfig getSpawn();
        SubMessageConfig getSign();
        StatusMessageConfig getStatus();
        TabMessageConfig getTab();

        interface SubMessageConfig {}

        interface FormatMessageConfig extends SubMessageConfig {

            SubFormatMessageConfig getColor();
            SubFormatMessageConfig getEmoji();
            SubFormatMessageConfig getFixation();
            SubFormatMessageConfig getImage();
            SubFormatMessageConfig getMention();
            ModerationFormatMessageConfig getModeration();
            SubFormatMessageConfig getName_();
            SubFormatMessageConfig getQuestionAnswer();
            SubFormatMessageConfig getSpoiler();
            SubFormatMessageConfig getTranslate();
            SubFormatMessageConfig getWorld();

            interface SubFormatMessageConfig {}

            interface ModerationFormatMessageConfig extends SubFormatMessageConfig {

                SubModerationFormatMessageConfig getCaps();
                SubModerationFormatMessageConfig getFlood();
                SubModerationFormatMessageConfig getSwear();

                interface SubModerationFormatMessageConfig {}

            }

        }

        interface ObjectiveMessageConfig extends SubMessageConfig {

            SubObjectiveMessageConfig getBelowname();
            SubObjectiveMessageConfig getTabname();

            interface SubObjectiveMessageConfig {}

        }

        interface StatusMessageConfig {

            SubStatusMessageConfig getMotd();
            SubStatusMessageConfig getIcon();
            SubStatusMessageConfig getPlayers();
            SubStatusMessageConfig getVersion();

            interface SubStatusMessageConfig {}

        }

        interface TabMessageConfig extends SubMessageConfig {

            SubTabMessageConfig getFooter();
            SubTabMessageConfig getHeader();
            SubTabMessageConfig getPlayerlistname();

            interface SubTabMessageConfig {}

        }
    }
}
