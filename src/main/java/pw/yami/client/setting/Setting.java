package pw.yami.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus.Internal;
import pw.yami.client.api.UnidentifiedInterface12;
import pw.yami.client.api.UnidentifiedInterface8;
import pw.yami.client.config.ConfigContainer;
import pw.yami.client.event.EventPhase;
import pw.yami.client.event.UnknownEvent3;
import pw.yami.client.manager.TextManager;
import pw.yami.eventbus.EventBusImpl;

public abstract class Setting implements UnidentifiedInterface8, UnidentifiedInterface12 {
   private final String text;
   private final String text2;
   private final Object g;
   protected Object h;
   private ConfigContainer configContainer;
   private Supplier supplier;
   private static String[] texts;

   public Setting(String str, String str2, Object obj) {
      if (obj == null) {
         throw new NullPointerException("Null values not supported!");
      }

      this.text = str;
      this.text2 = str2;
      this.g = obj;
      this.h = obj;
   }

   public Setting(String str, String str2, Object obj, Supplier supplier2) {
      this(str, str2, obj);
      this.supplier = supplier2;
   }

   @Internal
   public Setting(String str, String str2, long l) {
      this.text = str;
      this.text2 = str2;
      this.g = null;
   }

   @Override
   public JsonObject LT(int n, int n2, char c) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("name", this.getText());
      jsonObject.addProperty("id", this.Kn());
      return jsonObject;
   }

   @Override
   public Object LU(JsonObject jsonObject) {
      if (!jsonObject.has("value")) {
         return null;
      }

      JsonElement jsonElement = jsonObject.get("value");
      return jsonElement.getAsByte();
   }

   public String getText() {
      return this.text;
   }

   public String atu() {
      String trans = TextManager.aip(this.Kn());
      if (trans.equals(this.Kn()) || trans.endsWith("-config")) {
         return this.getText();
      }
      return trans;
   }

   @Override
   public String Kn() {
      Object[] objs = new Object[]{this.configContainer.getText2().toLowerCase(), this.text.toLowerCase()};
      return String.format("%s-%s-config", objs);
   }

   public String getText2() {
      return this.text2;
   }

   public String atw() {
      String trans = TextManager.aip(this.Kn() + ".description");
      if (trans.equals(this.Kn() + ".description") || trans.contains("-config.description")) {
         return this.getText2();
      }
      return trans;
   }

   public Object atx() {
      return this.h;
   }

   public void aty(Object obj) {
      if (obj == null) {
         throw new NullPointerException("Null values not supported!");
      }

      UnknownEvent3 unknownEvent3 = new UnknownEvent3(this);
      unknownEvent3.aqo(EventPhase.PRE);
      EventBusImpl.INSTANCE.rC(unknownEvent3);
      this.h = obj;
      unknownEvent3.aqo(EventPhase.POST);
      EventBusImpl.INSTANCE.rC(unknownEvent3);
   }

   public ConfigContainer getConfigContainer() {
      return this.configContainer;
   }

   public void setConfigContainer(ConfigContainer configContainer2) {
      this.configContainer = configContainer2;
   }

   public boolean isVisible() {
      return this.supplier != null ? (Boolean)this.supplier.get() : true;
   }

   public void atC(boolean flag) {
      this.supplier = () -> atE(flag);
   }

   public void atD() {
      this.aty(this.g);
   }

   private static Boolean atE(boolean flag) {
      return Boolean.valueOf(flag);
   }

   public static void b(String[] strs) {
      texts = strs;
   }

   public static String[] b() {
      return texts;
   }
}
