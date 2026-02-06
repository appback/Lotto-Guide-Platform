import React from 'react';
import { Typography, Divider, Space, Button } from 'antd';
import { Link } from 'react-router-dom';

const { Title, Paragraph, Text } = Typography;

export const AboutPage: React.FC = () => {
  return (
    <div style={{ padding: '24px', maxWidth: 1000, margin: '0 auto' }}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <div>
          <Title level={2}>소개 / 문의</Title>
          <Text type="secondary">시행일: 2026-02-05</Text>
        </div>

        <Divider />

        <Title level={4}>소개</Title>
        <Paragraph>
          <Text strong>Lotto-Guide(로또 가이드)</Text>는 로또 번호 선택에 도움을 줄 수 있는 간단한 번호 생성/가이드 도구입니다.
          부담 없이 번호를 생성해보고, 본인만의 기준으로 즐겁게 이용해 주세요.
        </Paragraph>
        <Paragraph>- 목적: 오락/참고용 도구 (당첨 보장 아님)</Paragraph>

        <Divider />

        <Title level={4}>문의</Title>
        <Paragraph>서비스 이용 중 문의/제휴/버그 제보는 아래로 연락해 주세요.</Paragraph>
        <Paragraph>
          - 이메일: <a href="mailto:appbackmaster@gmail.com">appbackmaster@gmail.com</a>
        </Paragraph>
        <Paragraph>가급적 2~3영업일 내 회신을 목표로 합니다.</Paragraph>

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
