import React from 'react';
import { Typography, Divider, Space, Button } from 'antd';
import { Link } from 'react-router-dom';

const { Title, Paragraph, Text } = Typography;

export const PrivacyPage: React.FC = () => {
  return (
    <div style={{ padding: '24px', maxWidth: 1000, margin: '0 auto' }}>
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <div>
          <Title level={2}>개인정보처리방침</Title>
          <Text type="secondary">시행일: 2026-02-05</Text>
        </div>

        <Divider />

        <Title level={4}>Lotto-Guide(로또 가이드) 개인정보처리방침</Title>
        <Paragraph>
          appback(이하 “운영자”)는 Lotto-Guide/로또 가이드(이하 “서비스”) 이용자의 개인정보를 중요하게 생각하며
          「개인정보 보호법」 등 관련 법령을 준수합니다. 본 개인정보처리방침은 서비스가 제공되는 웹사이트에서 적용됩니다.
        </Paragraph>

        <Title level={5}>1. 수집하는 개인정보 항목</Title>
        <Paragraph>
          서비스는 원칙적으로 <Text strong>회원가입 없이 이용</Text>할 수 있도록 제공됩니다. 다만 서비스 제공 과정에서
          아래 정보가 수집될 수 있습니다.
        </Paragraph>
        <Paragraph>
          (1) 이용자가 직접 제공하는 정보: 문의하기 이용 시 이메일 주소, 문의 내용(선택적으로 이름/닉네임)
        </Paragraph>
        <Paragraph>
          (2) 자동 수집 정보: 접속 로그, IP 주소, 쿠키, 방문 일시, 기기/브라우저 정보, 서비스 이용 기록
          (서비스 품질 개선, 보안, 통계 목적)
        </Paragraph>
        <Paragraph>※ 서비스는 원칙적으로 주민등록번호 등 민감정보를 수집하지 않습니다.</Paragraph>

        <Title level={5}>2. 개인정보의 수집 및 이용 목적</Title>
        <Paragraph>운영자는 수집한 개인정보를 다음 목적에 한해 이용합니다.</Paragraph>
        <Paragraph>- 문의 응대 및 민원 처리</Paragraph>
        <Paragraph>- 서비스 운영 및 품질 개선(오류 분석, 보안 대응, 통계)</Paragraph>
        <Paragraph>- 광고 제공 및 서비스 이용 분석(광고/분석 도구 사용 시)</Paragraph>

        <Title level={5}>3. 개인정보의 보유 및 이용 기간</Title>
        <Paragraph>- 문의 정보: 문의 처리 완료 후 1년 보관 후 파기(분쟁 대응 목적)</Paragraph>
        <Paragraph>- 접속 로그 등: 법령 또는 내부 정책에 따라 최대 1년 보관 후 파기</Paragraph>
        <Paragraph>단, 관계 법령에 따라 보존이 필요한 경우 해당 기간 동안 보관할 수 있습니다.</Paragraph>

        <Title level={5}>4. 개인정보의 제3자 제공</Title>
        <Paragraph>
          운영자는 원칙적으로 이용자의 개인정보를 제3자에게 제공하지 않습니다. 다만, 법령에 근거한 요청이 있는 경우 예외로 합니다.
        </Paragraph>

        <Title level={5}>5. 개인정보 처리 위탁</Title>
        <Paragraph>운영자는 원활한 서비스 운영을 위해 아래와 같이 개인정보 처리업무를 위탁할 수 있습니다.</Paragraph>
        <Paragraph>- 클라우드/서버: Amazon Web Services, Inc. (AWS) (데이터 저장 및 서비스 제공)</Paragraph>
        <Paragraph>- 광고: Google AdSense (Google LLC)</Paragraph>
        <Paragraph>※ 위탁 항목 및 업체는 운영 상황에 따라 변경될 수 있으며, 변경 시 본 방침에 반영합니다.</Paragraph>

        <Title level={5}>6. 쿠키 및 맞춤형 광고</Title>
        <Paragraph>
          서비스는 광고 제공을 위해 쿠키를 사용할 수 있습니다. Google은 쿠키를 사용하여 사용자의 이전 방문 기록을 바탕으로 광고를 게재할 수
          있습니다.
        </Paragraph>
        <Paragraph>
          사용자는 Google 광고 설정에서 맞춤형 광고를 관리/거부할 수 있습니다.
          <br />- https://adssettings.google.com/
          <br />- https://policies.google.com/technologies/ads
        </Paragraph>

        <Title level={5}>7. 이용자의 권리</Title>
        <Paragraph>
          이용자는 관련 법령에 따라 개인정보 열람, 정정, 삭제, 처리정지 등을 요청할 수 있습니다. 요청은 아래 문의처로 연락 바랍니다.
        </Paragraph>

        <Title level={5}>8. 개인정보의 안전성 확보 조치</Title>
        <Paragraph>
          운영자는 개인정보 보호를 위해 합리적인 보안 조치를 적용합니다. (접근 통제 및 최소 권한 관리, 전송 구간 암호화(HTTPS 적용 시),
          로그 모니터링 및 보안 업데이트 등)
        </Paragraph>

        <Title level={5}>9. 개인정보 보호책임자 및 문의처</Title>
        <Paragraph>
          - 개인정보 보호책임자: appback
          <br />- 이메일: appbackmaster@gmail.com
        </Paragraph>

        <Title level={5}>10. 고지의 의무</Title>
        <Paragraph>
          본 방침은 2026-02-05부터 적용됩니다. 내용 추가/삭제/수정이 있을 경우 웹사이트를 통해 공지합니다.
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
