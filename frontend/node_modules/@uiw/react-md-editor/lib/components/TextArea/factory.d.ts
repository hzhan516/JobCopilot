import React, { JSX } from 'react';
import { type ContextStore, type ExecuteCommandState } from '../../Context';
import { type TextAreaProps } from './Textarea';
import { type IProps } from '../../Types';
import { TextAreaCommandOrchestrator, type ICommand } from '../../commands/';
import './index.less';
export type RenderTextareaHandle = {
    dispatch: ContextStore['dispatch'];
    onChange?: TextAreaProps['onChange'];
    useContext?: {
        commands: ContextStore['commands'];
        extraCommands: ContextStore['extraCommands'];
        commandOrchestrator?: TextAreaCommandOrchestrator;
    };
    shortcuts?: (e: KeyboardEvent | React.KeyboardEvent<HTMLTextAreaElement>, commands: ICommand[], commandOrchestrator?: TextAreaCommandOrchestrator, dispatch?: React.Dispatch<ContextStore>, state?: ExecuteCommandState) => void;
};
export interface ITextAreaProps extends Omit<React.TextareaHTMLAttributes<HTMLTextAreaElement>, 'value' | 'onScroll'>, IProps {
    value?: string;
    onScroll?: (e: React.UIEvent<HTMLDivElement>) => void;
    renderTextarea?: (props: React.TextareaHTMLAttributes<HTMLTextAreaElement> | React.HTMLAttributes<HTMLDivElement>, opts: RenderTextareaHandle) => JSX.Element;
}
export type TextAreaRef = {
    text?: HTMLTextAreaElement;
    warp?: HTMLDivElement;
};
type MarkdownComponent = React.ComponentType<{
    prefixCls?: string;
}>;
export declare function createTextArea(options?: {
    Markdown?: MarkdownComponent;
    useMinHeight?: boolean;
}): (props: ITextAreaProps) => import("react/jsx-runtime").JSX.Element;
export {};
