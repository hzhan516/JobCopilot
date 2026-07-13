import { describe, it, expect, afterAll } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import { I18nextProvider } from 'react-i18next';
import { FileText } from 'lucide-react';
import i18n from '@/i18n';
import MasterDetailEmpty from './MasterDetailEmpty';

async function renderInLanguage(lang: string, titleKey: string, descKey: string) {
  await act(async () => {
    await i18n.changeLanguage(lang);
  });
  return render(
    <I18nextProvider i18n={i18n}>
      <MasterDetailEmpty icon={FileText} titleKey={titleKey} descKey={descKey} />
    </I18nextProvider>
  );
}

describe('MasterDetailEmpty i18n rendering', () => {
  afterAll(async () => {
    await i18n.changeLanguage('en');
  });

  it('renders the resume empty state in English', async () => {
    await renderInLanguage('en', 'masterDetail.resume.emptyTitle', 'masterDetail.resume.emptyDesc');
    expect(screen.getByText('Select a resume')).toBeInTheDocument();
    expect(screen.getByText('Choose a resume from the list to view details')).toBeInTheDocument();
  });

  it('renders the resume empty state in Simplified Chinese', async () => {
    await renderInLanguage(
      'zh-CN',
      'masterDetail.resume.emptyTitle',
      'masterDetail.resume.emptyDesc'
    );
    expect(screen.getByText('选择一份简历')).toBeInTheDocument();
    expect(screen.getByText('从左侧列表中选择一份简历以查看详情')).toBeInTheDocument();
  });

  it('renders the resume empty state in Traditional Chinese', async () => {
    await renderInLanguage(
      'zh-TW',
      'masterDetail.resume.emptyTitle',
      'masterDetail.resume.emptyDesc'
    );
    expect(screen.getByText('選擇一份履歷')).toBeInTheDocument();
    expect(screen.getByText('從左側列表中選擇一份履歷以查看詳情')).toBeInTheDocument();
  });

  it('renders the job empty state across languages', async () => {
    const { unmount: u1 } = await renderInLanguage(
      'en',
      'masterDetail.job.emptyTitle',
      'masterDetail.job.emptyDesc'
    );
    expect(screen.getByText('Select a job')).toBeInTheDocument();
    u1();

    const { unmount: u2 } = await renderInLanguage(
      'zh-CN',
      'masterDetail.job.emptyTitle',
      'masterDetail.job.emptyDesc'
    );
    expect(screen.getByText('选择一个职位')).toBeInTheDocument();
    u2();

    await renderInLanguage('zh-TW', 'masterDetail.job.emptyTitle', 'masterDetail.job.emptyDesc');
    expect(screen.getByText('選擇一個職位')).toBeInTheDocument();
  });

  it('renders the application empty state across languages', async () => {
    const { unmount: u1 } = await renderInLanguage(
      'en',
      'masterDetail.application.emptyTitle',
      'masterDetail.application.emptyDesc'
    );
    expect(screen.getByText('Select an application')).toBeInTheDocument();
    u1();

    const { unmount: u2 } = await renderInLanguage(
      'zh-CN',
      'masterDetail.application.emptyTitle',
      'masterDetail.application.emptyDesc'
    );
    expect(screen.getByText('选择一条申请记录')).toBeInTheDocument();
    u2();

    await renderInLanguage(
      'zh-TW',
      'masterDetail.application.emptyTitle',
      'masterDetail.application.emptyDesc'
    );
    expect(screen.getByText('選擇一筆申請記錄')).toBeInTheDocument();
  });
});
