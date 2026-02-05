import React, { useState } from 'react';
import { Tabs } from 'antd';
import { DeepGeneratePage } from './DeepGeneratePage';
import { GeneratePage } from './GeneratePage';
import { HistoryPage } from './HistoryPage';
import { AIGeneratePage } from './AIGeneratePage';

export const MainPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<string>('destiny');

  return (
    <div style={{ padding: '24px', maxWidth: '1400px', margin: '0 auto' }}>
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        type="card"
        size="large"
        items={[
          {
            key: 'destiny',
            label: '운명의 번호 추천',
            children: <DeepGeneratePage />,
          },
          {
            key: 'basic',
            label: '로또 번호 추천',
            children: <GeneratePage />,
          },
          {
            key: 'ai',
            label: 'AI 기반 추천 (유료)',
            children: <AIGeneratePage />,
          },
          {
            key: 'history',
            label: '히스토리',
            children: <HistoryPage />,
          },
        ]}
      />
    </div>
  );
};
