import { useEffect, useMemo, useState } from 'react';
import {
  Search,
  Settings,
  Edit2,
  RotateCcw,
  Lock,
  EyeOff,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';

import { useAdminStore } from '@/store/admin.store';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Checkbox } from '@/components/ui/checkbox';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import type { DynamicConfig } from '@/services/adminService';

function maskValue(value: string): string {
  if (!value) return '';
  if (value.length <= 4) return '****';
  return value.slice(0, 2) + '****' + value.slice(-2);
}

function parseValue(config: DynamicConfig): string | number | boolean {
  const raw = config.value;
  if (config.valueType === 'BOOLEAN') return raw === 'true';
  if (config.valueType === 'NUMBER') {
    const n = Number(raw);
    return Number.isNaN(n) ? raw : n;
  }
  return raw;
}

export default function AdminConfig() {
  const { t } = useTranslation();
  const [search, setSearch] = useState('');
  const [editing, setEditing] = useState<DynamicConfig | null>(null);
  const [editValue, setEditValue] = useState<string | number | boolean>('');
  const [resetting, setResetting] = useState<DynamicConfig | null>(null);

  const { configs, configsLoading, fetchConfigs, updateConfig, resetConfig } = useAdminStore();

  useEffect(() => {
    fetchConfigs().catch(() => toast.error(t('admin.config.loadError')));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return configs;
    return configs.filter(
      (c) =>
        c.key.toLowerCase().includes(term) ||
        c.category.toLowerCase().includes(term) ||
        c.description.toLowerCase().includes(term)
    );
  }, [configs, search]);

  const grouped = useMemo(() => {
    const map = new Map<string, DynamicConfig[]>();
    filtered.forEach((c) => {
      const list = map.get(c.category) ?? [];
      list.push(c);
      map.set(c.category, list);
    });
    return Array.from(map.entries()).sort(([a], [b]) => a.localeCompare(b));
  }, [filtered]);

  const openEdit = (config: DynamicConfig) => {
    setEditing(config);
    setEditValue(parseValue(config));
  };

  const closeEdit = () => {
    setEditing(null);
    setEditValue('');
  };

  const handleSave = async () => {
    if (!editing) return;
    try {
      await updateConfig(editing.key, editValue);
      toast.success(t('admin.config.saveSuccess'));
      closeEdit();
    } catch {
      toast.error(t('admin.config.saveError'));
    }
  };

  const handleReset = async () => {
    if (!resetting) return;
    try {
      await resetConfig(resetting.key);
      toast.success(t('admin.config.resetSuccess'));
    } catch {
      toast.error(t('admin.config.resetError'));
    } finally {
      setResetting(null);
    }
  };

  const renderEditControl = () => {
    if (!editing) return null;
    switch (editing.valueType) {
      case 'BOOLEAN':
        return (
          <div className="flex items-center gap-2 py-2">
            <Checkbox
              checked={editValue === true}
              onCheckedChange={(checked) => setEditValue(checked === true)}
            />
            <Label>{editValue === true ? t('common.yes') : t('common.no')}</Label>
          </div>
        );
      case 'NUMBER':
        return (
          <Input
            type="number"
            value={editValue as string | number}
            onChange={(e) => setEditValue(e.target.value === '' ? '' : Number(e.target.value))}
          />
        );
      case 'JSON':
        return (
          <textarea
            className="w-full min-h-[120px] rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            value={String(editValue)}
            onChange={(e) => setEditValue(e.target.value)}
          />
        );
      default:
        return (
          <Input
            value={String(editValue)}
            onChange={(e) => setEditValue(e.target.value)}
          />
        );
    }
  };

  const typeLabel: Record<string, string> = {
    BOOLEAN: t('admin.config.valueTypeBoolean'),
    NUMBER: t('admin.config.valueTypeNumber'),
    STRING: t('admin.config.valueTypeString'),
    JSON: t('admin.config.valueTypeJson'),
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold">{t('admin.config.title')}</h1>
        <div className="relative w-full max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <Input
            placeholder={t('admin.config.searchPlaceholder')}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9"
          />
        </div>
      </div>

      {configsLoading ? (
        <div className="text-gray-500">{t('common.loading')}</div>
      ) : grouped.length === 0 ? (
        <div className="text-gray-500">{t('admin.config.noConfigs')}</div>
      ) : (
        <div className="space-y-6">
          {grouped.map(([category, items]) => (
            <Card key={category}>
              <CardHeader>
                <CardTitle className="text-base flex items-center gap-2">
                  <Settings className="w-4 h-4" />
                  {category}
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead className="bg-gray-50 border-b">
                      <tr>
                        <th className="text-left p-3 font-medium">{t('admin.config.key')}</th>
                        <th className="text-left p-3 font-medium">{t('admin.config.value')}</th>
                        <th className="text-left p-3 font-medium">{t('admin.config.type')}</th>
                        <th className="text-left p-3 font-medium">{t('admin.config.readOnly')}</th>
                        <th className="text-right p-3 font-medium">{t('admin.monitoring.actions')}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {items.map((c) => (
                        <tr key={c.key} className="border-b last:border-none hover:bg-gray-50">
                          <td className="p-3 font-mono text-xs">{c.key}</td>
                          <td className="p-3">
                            {c.sensitive ? (
                              <span className="flex items-center gap-1 text-gray-500">
                                <EyeOff className="w-3.5 h-3.5" />
                                {maskValue(c.value)}
                              </span>
                            ) : (
                              <span className="max-w-xs truncate block">{c.value}</span>
                            )}
                          </td>
                          <td className="p-3">
                            <Badge variant="secondary">{typeLabel[c.valueType] ?? c.valueType}</Badge>
                          </td>
                          <td className="p-3">
                            {c.readOnly ? (
                              <Badge variant="outline" className="flex items-center gap-1 w-fit">
                                <Lock className="w-3 h-3" />
                                {t('common.yes')}
                              </Badge>
                            ) : (
                              <span className="text-gray-400">-</span>
                            )}
                          </td>
                          <td className="p-3 text-right space-x-2">
                            <Button
                              variant="outline"
                              size="sm"
                              disabled={c.readOnly}
                              onClick={() => openEdit(c)}
                            >
                              <Edit2 className="w-3.5 h-3.5 mr-1" />
                              {t('common.edit')}
                            </Button>
                            <Button
                              variant="outline"
                              size="sm"
                              disabled={c.readOnly}
                              onClick={() => setResetting(c)}
                            >
                              <RotateCcw className="w-3.5 h-3.5 mr-1" />
                              {t('admin.config.reset')}
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <Dialog open={!!editing} onOpenChange={(open) => !open && closeEdit()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('admin.config.editTitle')}</DialogTitle>
            {editing && (
              <DialogDescription className="font-mono text-xs">{editing.key}</DialogDescription>
            )}
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>{t('admin.config.value')}</Label>
              {renderEditControl()}
            </div>
            {editing && (
              <div className="text-xs text-gray-500">
                {t('admin.config.defaultValue')}: <span className="font-mono">{editing.defaultValue}</span>
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={closeEdit}>{t('common.cancel')}</Button>
            <Button onClick={handleSave}>{t('common.save')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={!!resetting} onOpenChange={(open) => !open && setResetting(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('common.confirm')}</DialogTitle>
            <DialogDescription>
              {resetting && t('admin.config.resetConfirm', { key: resetting.key })}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setResetting(null)}>{t('common.cancel')}</Button>
            <Button onClick={handleReset}>{t('common.confirm')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
