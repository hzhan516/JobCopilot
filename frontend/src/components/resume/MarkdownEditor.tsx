import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import MDEditor from '@uiw/react-md-editor';
import { Button } from '../ui/button';
import { Save, X, Loader2 } from 'lucide-react';

interface MarkdownEditorProps {
  initialContent: string;
  versionId: string;
  onSave: (content: string) => void;
  onCancel: () => void;
  /**
   * 自动保存回调 / Auto-save callback
   */
  onAutoSave?: (content: string) => Promise<void>;
}

export const MarkdownEditor: React.FC<MarkdownEditorProps> = ({
  initialContent,
  versionId,
  onSave,
  onCancel,
  onAutoSave,
}) => {
  const { t } = useTranslation();
  const storageKey = `resume-editor-autosave-${versionId}`;

  const [content, setContent] = useState<string>(() => {
    const saved = localStorage.getItem(storageKey);
    return saved !== null ? saved : initialContent;
  });

  const [autoSaveStatus, setAutoSaveStatus] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle');
  const autoSaveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // 本地自动备份到 localStorage / Local auto-backup to localStorage
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      localStorage.setItem(storageKey, content);
    }, 1000);
    return () => clearTimeout(timeoutId);
  }, [content, storageKey]);

  // Debounce 自动保存到后端 / Debounce auto-save to backend
  useEffect(() => {
    if (!onAutoSave) return;
    if (autoSaveTimerRef.current) {
      clearTimeout(autoSaveTimerRef.current);
    }
    // 重置自动保存状态 / Reset auto-save status
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setAutoSaveStatus('idle');
    autoSaveTimerRef.current = setTimeout(async () => {
      setAutoSaveStatus('saving');
      try {
        await onAutoSave(content);
        setAutoSaveStatus('saved');
        localStorage.removeItem(storageKey);
      } catch {
        setAutoSaveStatus('error');
      }
    }, 3000);
    return () => {
      if (autoSaveTimerRef.current) {
        clearTimeout(autoSaveTimerRef.current);
      }
    };
  }, [content, onAutoSave, storageKey]);

  const handleSave = useCallback(() => {
    onSave(content);
    localStorage.removeItem(storageKey);
  }, [content, onSave, storageKey]);

  const handleCancel = useCallback(() => {
    localStorage.removeItem(storageKey);
    onCancel();
  }, [onCancel, storageKey]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault();
        handleSave();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleSave]);

  const getStatusIndicator = () => {
    switch (autoSaveStatus) {
      case 'saving':
        return (
          <span className="flex items-center text-xs text-muted-foreground">
            <Loader2 className="w-3 h-3 mr-1 animate-spin" />
            {t('resume.markdownEditor.autoSaving')}
          </span>
        );
      case 'saved':
        return (
          <span className="text-xs text-green-600">
            {t('resume.markdownEditor.autoSaveSuccess')}
          </span>
        );
      case 'error':
        return (
          <span className="text-xs text-red-500">
            {t('resume.markdownEditor.autoSaveError')}
          </span>
        );
      default:
        return null;
    }
  };

  return (
    <div className="flex flex-col h-full w-full space-y-4" data-color-mode="light">
      <div className="flex justify-between items-center">
        <div className="flex items-center space-x-3">
          <h3 className="text-lg font-semibold">{t('resume.markdownEditor.title')}</h3>
          {getStatusIndicator()}
        </div>
        <div className="flex space-x-2">
          <Button variant="outline" onClick={handleCancel}>
            <X className="w-4 h-4 mr-2" />
            {t('resume.markdownEditor.cancel')}
          </Button>
          <Button onClick={handleSave}>
            <Save className="w-4 h-4 mr-2" />
            {t('resume.markdownEditor.save')}
          </Button>
        </div>
      </div>

      <div className="flex-grow border rounded-md overflow-hidden">
        <MDEditor
          value={content}
          onChange={(val) => setContent(val || '')}
          preview="live"
          height="100%"
          className="h-full min-h-[500px]"
          visibleDragbar={false}
        />
      </div>
    </div>
  );
};
