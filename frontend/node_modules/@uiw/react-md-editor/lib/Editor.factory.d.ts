import React from 'react';
import { type ContextStore } from './Context';
import type { MDEditorProps } from './Types';
export interface RefMDEditor extends ContextStore {
}
type PreviewComponent = React.ComponentType<any>;
type TextAreaComponent = React.ComponentType<any>;
export declare function createMDEditor<TMarkdownPreview extends PreviewComponent, TTextArea extends TextAreaComponent>(options: {
    MarkdownPreview: TMarkdownPreview;
    TextArea: TTextArea;
}): React.ForwardRefExoticComponent<MDEditorProps & React.RefAttributes<RefMDEditor>> & {
    Markdown: TMarkdownPreview;
};
export {};
