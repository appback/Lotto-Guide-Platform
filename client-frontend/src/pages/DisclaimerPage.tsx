import React from 'react';
import { Typography, Divider, Space, Button } from 'antd';
import { Link } from 'react-router-dom';

const { Title, Paragraph, Text } = Typography;

export const DisclaimerPage: React.FC = () => {
  return (
    <div style={{ padding: '24px', maxWidth: 1000, margin: '0 auto' }}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <div>
          <Title level={2}>면책/주의사항</Title>
          <Text type="secondary">시행일: 2026-02-05</Text>
        </div>

        <Divider />

        <Paragraph>
          Lotto-Guide(로또 가이드)는 로또 번호 선택에 참고가 될 수 있는 기능(번호 생성/추천/통계 등)을 제공하나, 이는 <Text strong>오락 및 참고 목적</Text>
          입니다.
        </Paragraph>

        <Paragraph>1. 서비스가 제공하는 번호 및 정보는 당첨을 보장하지 않습니다.</Paragraph>
        <Paragraph>
          2. 서비스의 결과(번호 추천, 통계 등)는 확률/과거 데이터/알고리즘 기반일 수 있으나, 실제 추첨 결과와는 무관할 수 있습니다.
        </Paragraph>
        <Paragraph>3. 이용자의 구매/선택/행동에 대한 최종 책임은 이용자에게 있습니다.</Paragraph>
        <Paragraph>
          4. 본 서비스는 로또 판매/대행 또는 도박 행위를 조장하기 위한 목적이 아니며, 관련 법령을 준수합니다.
        </Paragraph>

        <Divider />
        <Space>
          <Button type="primary">
            <Link to="/">홈으로</Link>
          </Button>
        </Space>
      </Space>
    </div>
  );
};
