import React from 'react';
import type { ResumeVersion } from '../../types/resume';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';
import { Edit3, FileText, Briefcase, List } from 'lucide-react';

interface VersionDetailProps {
  version: ResumeVersion;
  onEdit: () => void;
}

export const VersionDetail: React.FC<VersionDetailProps> = ({ version, onEdit }) => {
  const { parsedContent, content, versionType, status, parseStatus } = version;

  const getStatusBadge = () => {
    switch (parseStatus) {
      case 'COMPLETED':
        return <Badge variant="default" className="bg-green-500">Parsed</Badge>;
      case 'PROCESSING':
        return <Badge variant="secondary" className="bg-blue-500 text-white">Processing</Badge>;
      case 'FAILED':
        return <Badge variant="destructive">Failed</Badge>;
      default:
        return <Badge variant="outline">Pending</Badge>;
    }
  };

  const getTypeBadge = () => {
    switch (versionType) {
      case 'ORIGINAL':
        return <Badge variant="outline" className="border-blue-200 text-blue-700 bg-blue-50">Original</Badge>;
      case 'CONVERTED':
        return <Badge variant="outline" className="border-purple-200 text-purple-700 bg-purple-50">Converted</Badge>;
      case 'AI_OPTIMIZED':
        return <Badge variant="outline" className="border-amber-200 text-amber-700 bg-amber-50">AI Optimized</Badge>;
      default:
        return null;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div className="flex items-center space-x-3">
          <h2 className="text-2xl font-bold tracking-tight">Version Details</h2>
          {getTypeBadge()}
          {getStatusBadge()}
          {status === 'ARCHIVED' && <Badge variant="secondary">Archived</Badge>}
        </div>
        
        {versionType !== 'ORIGINAL' && (
          <Button onClick={onEdit} variant="outline" size="sm">
            <Edit3 className="w-4 h-4 mr-2" />
            Edit Content
          </Button>
        )}
      </div>

      {parsedContent && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-lg flex items-center">
                <Briefcase className="w-5 h-5 mr-2 text-muted-foreground" />
                Basic Information
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <h4 className="text-sm font-medium text-muted-foreground mb-1">Title</h4>
                <p className="text-base font-medium">{parsedContent.title || 'Not specified'}</p>
              </div>
              <div>
                <h4 className="text-sm font-medium text-muted-foreground mb-1">Company</h4>
                <p className="text-base">{parsedContent.company || 'Not specified'}</p>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-lg flex items-center">
                <List className="w-5 h-5 mr-2 text-muted-foreground" />
                Requirements
              </CardTitle>
            </CardHeader>
            <CardContent>
              {parsedContent.requirements && parsedContent.requirements.length > 0 ? (
                <ul className="list-disc pl-5 space-y-1">
                  {parsedContent.requirements.map((req, index) => (
                    <li key={index} className="text-sm">{req}</li>
                  ))}
                </ul>
              ) : (
                <p className="text-sm text-muted-foreground italic">No requirements extracted.</p>
              )}
            </CardContent>
          </Card>

          <Card className="md:col-span-2">
            <CardHeader className="pb-3">
              <CardTitle className="text-lg flex items-center">
                <FileText className="w-5 h-5 mr-2 text-muted-foreground" />
                Description
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm whitespace-pre-wrap">
                {parsedContent.description || 'No description available.'}
              </p>
            </CardContent>
          </Card>
        </div>
      )}

      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-lg flex items-center">
            <FileText className="w-5 h-5 mr-2 text-muted-foreground" />
            Raw Content
          </CardTitle>
        </CardHeader>
        <CardContent>
          {content ? (
            <div className="bg-muted/50 p-4 rounded-md overflow-auto max-h-[500px]">
              <pre className="text-sm whitespace-pre-wrap font-mono">{content}</pre>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground italic text-center py-8">
              Content is not available for this version.
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
};
