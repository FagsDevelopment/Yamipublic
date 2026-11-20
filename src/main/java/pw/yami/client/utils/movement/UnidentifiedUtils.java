package pw.yami.client.utils.movement;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class UnidentifiedUtils {

   private static Object getPrimaryBaritone() {
      try {
         Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
         Method getProvider = apiClass.getMethod("getProvider");
         Object provider = getProvider.invoke(null);
         if (provider == null) {
            return null;
         }
         Method getPrimary = provider.getClass().getMethod("getPrimaryBaritone");
         return getPrimary.invoke(provider);
      } catch (Throwable ignored) {
         return null;
      }
   }

   public static boolean eP() {
      try {
         Object baritone = getPrimaryBaritone();
         if (baritone != null) {
            Method getPathing = baritone.getClass().getMethod("getPathingBehavior");
            Object pathing = getPathing.invoke(baritone);
            if (pathing != null) {
               Method isPathing = pathing.getClass().getMethod("isPathing");
               return (Boolean) isPathing.invoke(pathing);
            }
         }
      } catch (Throwable ignored) {
      }
      return false;
   }

   public static void eQ() {
      try {
         Object baritone = getPrimaryBaritone();
         if (baritone != null) {
            Method getPathing = baritone.getClass().getMethod("getPathingBehavior");
            Object pathing = getPathing.invoke(baritone);
            if (pathing != null) {
               Method cancel = pathing.getClass().getMethod("forceCancel");
               cancel.invoke(pathing);
            }
         }
      } catch (Throwable ignored) {
      }
   }

   public static void eR(int n, int n2, int n3) {
      try {
         Object baritone = getPrimaryBaritone();
         if (baritone != null) {
            Method getCustomGoal = baritone.getClass().getMethod("getCustomGoalProcess");
            Object process = getCustomGoal.invoke(baritone);
            if (process != null) {
               Class<?> goalBlockClass = Class.forName("baritone.api.pathing.goals.GoalBlock");
               Constructor<?> ctor = goalBlockClass.getConstructor(int.class, int.class, int.class);
               Object goal = ctor.newInstance(n, n2, n3);
               Class<?> goalClass = Class.forName("baritone.api.pathing.goals.Goal");
               Method setGoal = process.getClass().getMethod("setGoalAndPath", goalClass);
               setGoal.invoke(process, goal);
            }
         }
      } catch (Throwable ignored) {
      }
   }

   public static boolean eS() {
      try {
         Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
         Method getSettings = apiClass.getMethod("getSettings");
         Object settings = getSettings.invoke(null);
         if (settings != null) {
            Field freeLookField = settings.getClass().getField("freeLook");
            Object freeLookSetting = freeLookField.get(settings);
            if (freeLookSetting != null) {
               Field valueField = freeLookSetting.getClass().getField("value");
               return (Boolean) valueField.get(freeLookSetting);
            }
         }
      } catch (Throwable ignored) {
      }
      return false;
   }
}
