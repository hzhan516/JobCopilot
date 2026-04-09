import React, { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { resumeService } from '@/services/resumeService';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Progress } from '@/components/ui/progress';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Upload, File, X, Loader2, CheckCircle, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';

const ALLOWED_TYPES = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/markdown',
  'text/plain',
];

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

export default function ResumeUpload() {
  const navigate = useNavigate();
  const [file, setFile] = useState<File | null>(null);
  const [title, setTitle] = useState('');
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [error, setError] = useState('');

  // 验证文件
  const validateFile = (file: File): string | null => {
    if (!ALLOWED_TYPES.includes(file.type)) {
      return '不支持的文件格式，请上传 PDF、DOCX、MD 或 TXT 文件';
    }
    if (file.size > MAX_FILE_SIZE) {
      return '文件大小超过 10MB 限制';
    }
    return null;
  };

  // 处理文件选择
  const handleFileSelect = (selectedFile: File) => {
    setError('');
    const validationError = validateFile(selectedFile);
    if (validationError) {
      setError(validationError);
      return;
    }
    setFile(selectedFile);
    // 自动设置标题为文件名（不含扩展名）
    if (!title) {
      const fileName = selectedFile.name.replace(/\.[^/.]+$/, '');
      setTitle(fileName);
    }
  };

  // 处理拖放
  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    const droppedFile = e.dataTransfer.files[0];
    if (droppedFile) {
      handleFileSelect(droppedFile);
    }
  }, []);

  // 处理上传
  const handleUpload = async () => {
    if (!file) {
      setError('请选择要上传的文件');
      return;
    }

    setIsUploading(true);
    setUploadProgress(0);
    setError('');

    // 模拟进度
    const progressInterval = setInterval(() => {
      setUploadProgress((prev) => {
        if (prev >= 90) return prev;
        return prev + 10;
      });
    }, 500);

    try {
      await resumeService.uploadResume(file, title);
      clearInterval(progressInterval);
      setUploadProgress(100);
      toast.success('简历上传成功');
      setTimeout(() => {
        navigate('/resumes');
      }, 500);
    } catch (err: any) {
      clearInterval(progressInterval);
      setError(err.response?.data?.message || '上传失败，请稍后重试');
      setIsUploading(false);
    }
  };

  // 清除选择的文件
  const clearFile = () => {
    setFile(null);
    setTitle('');
    setError('');
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* 页面标题 */}
      <div>
        <h1 className="text-3xl font-bold text-gray-900">上传简历</h1>
        <p className="text-gray-500 mt-1">支持 PDF、Word、Markdown 格式</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>选择文件</CardTitle>
          <CardDescription>上传后系统将自动解析并生成多个版本</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {error && (
            <Alert variant="destructive">
              <AlertCircle className="w-4 h-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {/* 拖放区域 */}
          {!file ? (
            <div
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              className={`border-2 border-dashed rounded-xl p-12 text-center transition-colors ${
                isDragging
                  ? 'border-blue-500 bg-blue-50'
                  : 'border-gray-300 hover:border-gray-400'
              }`}
            >
              <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <Upload className="w-8 h-8 text-blue-600" />
              </div>
              <p className="text-lg font-medium text-gray-900 mb-2">
                点击或拖拽文件到此处
              </p>
              <p className="text-sm text-gray-500 mb-4">
                支持 PDF、DOCX、MD、TXT 格式，最大 10MB
              </p>
              <Input
                type="file"
                accept=".pdf,.docx,.md,.txt"
                onChange={(e) => {
                  const selectedFile = e.target.files?.[0];
                  if (selectedFile) {
                    handleFileSelect(selectedFile);
                  }
                }}
                className="hidden"
                id="file-input"
              />
              <Label htmlFor="file-input">
                <Button variant="outline" className="cursor-pointer" asChild>
                  <span>选择文件</span>
                </Button>
              </Label>
            </div>
          ) : (
            <div className="border rounded-xl p-6">
              <div className="flex items-start justify-between">
                <div className="flex items-center space-x-4">
                  <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                    <File className="w-6 h-6 text-blue-600" />
                  </div>
                  <div>
                    <p className="font-medium text-gray-900">{file.name}</p>
                    <p className="text-sm text-gray-500">
                      {(file.size / 1024).toFixed(1)} KB
                    </p>
                  </div>
                </div>
                {!isUploading && (
                  <Button variant="ghost" size="icon" onClick={clearFile}>
                    <X className="w-5 h-5" />
                  </Button>
                )}
              </div>

              {isUploading && (
                <div className="mt-4">
                  <div className="flex items-center justify-between text-sm mb-2">
                    <span className="text-gray-600">上传中...</span>
                    <span className="text-gray-900">{uploadProgress}%</span>
                  </div>
                  <Progress value={uploadProgress} className="h-2" />
                </div>
              )}

              {uploadProgress === 100 && (
                <div className="mt-4 flex items-center text-green-600">
                  <CheckCircle className="w-5 h-5 mr-2" />
                  <span>上传成功！</span>
                </div>
              )}
            </div>
          )}

          {/* 标题输入 */}
          {file && !isUploading && (
            <div className="space-y-2">
              <Label htmlFor="title">简历标题（可选）</Label>
              <Input
                id="title"
                placeholder="输入简历标题"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
              />
            </div>
          )}

          {/* 操作按钮 */}
          <div className="flex space-x-4">
            <Button
              variant="outline"
              onClick={() => navigate('/resumes')}
              disabled={isUploading}
              className="flex-1"
            >
              取消
            </Button>
            <Button
              onClick={handleUpload}
              disabled={!file || isUploading}
              className="flex-1"
            >
              {isUploading ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  上传中...
                </>
              ) : (
                <>
                  <Upload className="w-4 h-4 mr-2" />
                  开始上传
                </>
              )}
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* 提示信息 */}
      <div className="bg-blue-50 rounded-lg p-4">
        <h4 className="text-sm font-medium text-blue-900 mb-2">上传说明</h4>
        <ul className="text-sm text-blue-700 space-y-1 list-disc list-inside">
          <li>上传后系统将自动解析简历内容</li>
          <li>系统会生成三个版本：原版、Markdown转换版、AI优化版</li>
          <li>您可以在编辑页面查看和修改各个版本</li>
          <li>所有简历数据都会安全存储</li>
        </ul>
      </div>
    </div>
  );
}
