package com.zte.toolsdk.producer;

/* loaded from: classes2.dex */
public class ToolsParams {
    private final AssistantState assistantState;
    private final boolean finish;
    private final boolean longRunningWork;
    private final boolean playSpeech;
    private final String response;
    private final boolean showChatWindow;
    private final String text;
    private final String toUserContent;
    private final ToolState toolState;

    public enum AssistantState {
        UNKNOWN,
        SHOW_CHAT_WINDOW,
        HIDE_WINDOW_KEEP_ASR,
        HIDE_WINDOW_STOP_ASR
    }

    public enum ToolState {
        COMPLETE,
        WORK_IN_PROCESS,
        CANCEL
    }

    private ToolsParams(Builder builder) {
        this.text = builder.text;
        this.response = builder.response;
        this.finish = builder.finish;
        this.showChatWindow = builder.showChatWindow;
        this.assistantState = builder.assistantState;
        this.playSpeech = builder.playSpeech;
        this.toUserContent = builder.toUserContent;
        this.toolState = builder.toolState;
        this.longRunningWork = builder.longRunningWork;
    }

    public String getText() {
        return this.text;
    }

    public String getResponse() {
        return this.response;
    }

    public boolean getFinish() {
        return this.finish;
    }

    public boolean getShowChatWindow() {
        return this.showChatWindow;
    }

    public AssistantState getAssistantState() {
        return this.assistantState;
    }

    public boolean getPlaySpeech() {
        return this.playSpeech;
    }

    public String getToUserContent() {
        return this.toUserContent;
    }

    public ToolState getToolState() {
        return this.toolState;
    }

    public boolean getLongRunningWork() {
        return this.longRunningWork;
    }

    public static class Builder {
        private String text = null;
        private String response = null;
        private boolean finish = false;
        private boolean showChatWindow = true;
        private AssistantState assistantState = AssistantState.UNKNOWN;
        private boolean playSpeech = true;
        private String toUserContent = null;
        private ToolState toolState = ToolState.COMPLETE;
        private boolean longRunningWork = false;

        public Builder setText(String text) {
            this.text = text;
            return this;
        }

        public Builder setResponse(String response) {
            this.response = response;
            return this;
        }

        public Builder setFinish(boolean finish) {
            this.finish = finish;
            return this;
        }

        public Builder setShowChatWindow(boolean showChatWindow) {
            this.showChatWindow = showChatWindow;
            return this;
        }

        public Builder setAssistantState(AssistantState assistantState) {
            this.assistantState = assistantState;
            return this;
        }

        public Builder setPlaySpeech(boolean playSpeech) {
            this.playSpeech = playSpeech;
            return this;
        }

        public Builder setToUserContent(String toUserContent) {
            this.toUserContent = toUserContent;
            return this;
        }

        public Builder setToolState(ToolState toolState) {
            this.toolState = toolState;
            return this;
        }

        public Builder setLongRunningWork(boolean longRunningWork) {
            this.longRunningWork = longRunningWork;
            return this;
        }

        public ToolsParams build() {
            return new ToolsParams(this);
        }
    }
}
