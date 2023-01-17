package com.revelation.Cooking;

import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CookingMod implements WurmServerMod, ServerStartedListener {

    public static Logger logger = Logger.getLogger(CookingMod.class.getName());
    public static void logException(String msg, Throwable e) {
        if (logger != null)
            logger.log(Level.SEVERE, msg, e);
    }
    public static void logWarning(String msg) {
        if (logger != null)
            logger.log(Level.WARNING, msg);
    }
    public static void logInfo(String msg) {
        if (logger != null)
            logger.log(Level.INFO, msg);
    }

    public void onServerStarted(){
        ModActions.registerBehaviourProvider(new CookingFillActions());
    }
}
