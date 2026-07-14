package com.ms.middleware.autonomy.adoption.nacos;

/**
 * 将 suggestedConfig 合并进现有 YAML，生成草稿内容与 diff 摘要。
 */
final class NacosConfigDraftContentBuilder {

    private NacosConfigDraftContentBuilder() {
    }

    /**
     * @param currentConfig 生产 dataId 当前内容，可为 null/空
     * @param suggestedConfig 推荐片段
     * @return [0]=draftContent, [1]=diffSummary
     */
    static String[] build(String currentConfig, String suggestedConfig) {
        String base = currentConfig != null ? currentConfig.trim() : "";
        String suggestion = suggestedConfig != null ? suggestedConfig.trim() : "";
        String header = "# --- ms-autonomy draft (未发布生产，需控制台二次确认) ---";
        String block = header + "\n" + suggestion;
        String draftContent = base.isEmpty() ? block : base + "\n\n" + block;
        String diffSummary = "+ " + suggestion.replace("\n", "\n+ ");
        return new String[]{draftContent, diffSummary};
    }
}
