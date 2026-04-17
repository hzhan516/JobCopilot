import React, { useState, useEffect, useCallback } from 'react';
import MDEditor from '@uiw/react-md-editor';
import { Button } from '../ui/button';
import { Save, X } from 'lucide-react';

interface MarkdownEditorProps {
  initialContent: string;
  versionId: string;
  onSave: (content: string) => void;
  onCancel: () => void;
}

export const MarkdownEditor: React.FC<MarkdownEditorProps> = ({
  initialContent,
  versionId,
  onSave,
  onCancel,
}) => {
  const storageKey = `resume-editor-autosave-${versionId}`;
  
  const [content, setContent] = useState<string>(() => {
    const saved = localStorage.getItem(storageKey);
    return saved !== null ? saved : initialContent;
  });

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      localStorage.setItem(storageKey, content);
    }, 1000);
    return () => clearTimeout(timeoutId);
  }, [content, storageKey]);

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

  return (
    <div className="flex flex-col h-full w-full space-y-4" data-color-mode="light">
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold">Edit Resume</h3>
        <div className="flex space-x-2">
          <Button variant="outline" onClick={handleCancel}>
            <X className="w-4 h-4 mr-2" />
            Cancel
          </Button>
          <Button onClick={handleSave}>
            <Save className="w-4 h-4 mr-2" />
            Save
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
