import React from 'react';
import { Typography, Divider, Space, Button } from 'antd';
import { Link } from 'react-router-dom';

const { Title, Paragraph, Text } = Typography;

export const TermsPage: React.FC = () => {
  return (
    <div style={{ padding: '24px', maxWidth: 1000, margin: '0 auto' }}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <div>
          <Title level={2}>이용약관</Title>
          <Text type="secondary">시행일: 2026-02-05</Text>
        </div>

        <Divider />

        <Paragraph>
          본 약관은 appback(이하 “운영자”)이 제공하는 Lotto-Guide/로또 가이드(이하 “서비스”)의 이용조건 및 절차, 권리·의무 및
          책임사항을 규정합니다.
        </Paragraph>

        <Title level={5}>제1조(목적)</Title>
        <Paragraph>본 약관은 서비스 이용과 관련하여 운영자와 이용자 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.</Paragraph>

        <Title level={5}>제2조(정의)</Title>
        <Paragraph>- “이용자”란 본 약관에 따라 서비스를 이용하는 자를 말합니다.</Paragraph>
        <Paragraph>- “콘텐츠”란 서비스가 제공하는 번호 생성/추천, 통계/분석 정보, 텍스트/이미지 등 일체를 말합니다.</Paragraph>

        <Title level={5}>제3조(약관의 효력 및 변경)</Title>
        <Paragraph>1. 운영자는 본 약관의 내용을 웹사이트에 게시함으로써 효력을 발생합니다.</Paragraph>
        <Paragraph>2. 운영자는 관련 법령을 위배하지 않는 범위에서 약관을 변경할 수 있으며, 변경 시 공지합니다.</Paragraph>
        <Paragraph>3. 변경된 약관에 동의하지 않을 경우 이용자는 서비스 이용을 중단할 수 있습니다.</Paragraph>

        <Title level={5}>제4조(서비스 제공)</Title>
        <Paragraph>- 로또 번호 생성/추천</Paragraph>
        <Paragraph>- (선택) 생성 이력 제공</Paragraph>
        <Paragraph>- 기타 운영자가 정하는 기능</Paragraph>
        <Paragraph>운영자는 운영상/기술상 필요에 따라 서비스 내용을 변경할 수 있습니다.</Paragraph>

        <Title level={5}>제5조(이용자의 의무)</Title>
        <Paragraph>이용자는 다음 행위를 하여서는 안 됩니다.</Paragraph>
        <Paragraph>- 서비스의 정상 운영을 방해하는 행위(과도한 요청, 자동화 공격 등)</Paragraph>
        <Paragraph>- 불법적 목적의 이용</Paragraph>
        <Paragraph>- 운영자 또는 제3자의 권리를 침해하는 행위</Paragraph>

        <Title level={5}>제6조(지적재산권)</Title>
        <Paragraph>
          서비스가 제공하는 콘텐츠 및 소프트웨어에 대한 권리는 운영자 또는 정당한 권리자에게 귀속됩니다. 이용자는 운영자의 사전 동의 없이
          무단 복제/배포/상업적 이용을 할 수 없습니다. (단, 법령상 허용되는 범위는 예외)
        </Paragraph>

        <Title level={5}>제7조(책임 제한)</Title>
        <Paragraph>1. 서비스는 무료로 제공되는 정보/기능에 대해 법령상 허용되는 범위 내에서 책임을 제한합니다.</Paragraph>
        <Paragraph>2. 서비스는 이용자의 기대수익, 당첨 여부 등 결과를 보장하지 않습니다.</Paragraph>
        <Paragraph>3. 운영자는 천재지변, 시스템 장애, 통신 장애 등 불가항력 사유로 서비스를 제공할 수 없는 경우 책임을 지지 않습니다.</Paragraph>

        <Title level={5}>제8조(준거법 및 관할)</Title>
        <Paragraph>본 약관은 대한민국 법령에 따르며, 분쟁이 발생할 경우 민사소송법상의 관할법원에 따릅니다.</Paragraph>

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
