interface AssistantPanelActionsProps {
  hasChat: boolean
  onExpand: () => void
  onClear: () => void
}

export function AssistantPanelActions({ hasChat, onExpand, onClear }: AssistantPanelActionsProps) {
  return (
    <>
      <button type="button" className="btn-ghost-sm" onClick={onExpand} title="放大助手">
        放大
      </button>
      {hasChat ? (
        <button type="button" className="btn-ghost-sm" onClick={onClear}>
          清空
        </button>
      ) : null}
    </>
  )
}
