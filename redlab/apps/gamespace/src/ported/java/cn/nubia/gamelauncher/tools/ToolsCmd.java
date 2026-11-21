package dev.redlab.gamespace.tools;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.annotations.JsonAdapter;
import java.lang.reflect.Type;

/* loaded from: classes.dex */
public class ToolsCmd {

    @JsonAdapter(ArgumentsAdapter.class)
    private Arguments arguments;
    private String name;

    public String getName() {
        return this.name;
    }

    public String getArgumentsAction() {
        Arguments arguments = this.arguments;
        if (arguments != null) {
            return arguments.getAction();
        }
        return null;
    }

    public String getArgumentsName() {
        Arguments arguments = this.arguments;
        if (arguments != null) {
            return arguments.getName();
        }
        return null;
    }

    public static class Arguments {
        private String action;
        private String name;

        public String getAction() {
            return this.action;
        }

        public void setAction(String str) {
            this.action = str;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String str) {
            this.name = str;
        }
    }

    private static class ArgumentsAdapter implements JsonDeserializer<Arguments> {
        private ArgumentsAdapter() {
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // com.google.gson.JsonDeserializer
        public Arguments deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            return (Arguments) jsonDeserializationContext.deserialize(JsonParser.parseString(jsonElement.getAsString().trim()), Arguments.class);
        }
    }
}
