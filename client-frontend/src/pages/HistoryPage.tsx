import React, { useState, useEffect, useMemo } from 'react';
import { Card, Table, Space, message, Pagination, Radio } from 'antd';
import { historyService } from '@/services/historyService';
import type { HistoryItemDto } from '@/types/api';
// import { useNavigate } from 'react-router-dom'; // 탭 기반으로 변경되어 제거
import type { ColumnsType } from 'antd/es/table';
import { LottoBallGroup } from '@/components/LottoBall';

const STORAGE_KEY_BALL_SIZE = 'lotto_ball_size';
type BallSize = 'small' | 'medium' | 'large';

export const HistoryPage: React.FC = () => {
  // const navigate = useNavigate(); // 탭 기반으로 변경되어 제거
  const [loading, setLoading] = useState(false);
  const [history, setHistory] = useState<HistoryItemDto[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  
  // 볼 크기 상태 (localStorage에서 불러오기)
  const [ballSize, setBallSize] = useState<BallSize>(() => {
    const saved = localStorage.getItem(STORAGE_KEY_BALL_SIZE);
    return (saved === 'small' || saved === 'medium' || saved === 'large') ? saved : 'medium';
  });

  useEffect(() => {
    loadHistory();
  }, [page, size]);

  const loadHistory = async () => {
    setLoading(true);
    try {
      const response = await historyService.getHistory(page, size);
      setHistory(response.items);
      setTotal(response.total);
    } catch (error: any) {
      console.error('히스토리 조회 오류:', error);
      message.error(error?.message || '히스토리 조회에 실패했습니다.');
      // 에러 발생 시 빈 배열로 설정
      setHistory([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  // 볼 크기 변경 핸들러
  const handleBallSizeChange = (e: any) => {
    const newSize = e.target.value as BallSize;
    setBallSize(newSize);
    localStorage.setItem(STORAGE_KEY_BALL_SIZE, newSize);
  };

  // ballSize가 변경될 때 columns 재생성
  const columns: ColumnsType<HistoryItemDto> = useMemo(() => [
    {
      title: 'ID',
      dataIndex: 'setId',
      key: 'setId',
      width: 80,
    },
    {
      title: '전략',
      dataIndex: 'strategy',
      key: 'strategy',
      width: 120,
    },
    {
      title: '생성 개수',
      dataIndex: 'generatedCount',
      key: 'generatedCount',
      width: 100,
    },
    {
      title: '생성 시간',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (text: string) => new Date(text).toLocaleString('ko-KR'),
    },
    {
      title: '번호',
      key: 'numbers',
      render: (_, record) => (
        <Space direction="vertical" size="small">
          {record.numbers.map((numSet, index) => (
            <LottoBallGroup 
              key={index}
              numbers={numSet.numbers} 
              size={ballSize}
              animated={false}
              gap={ballSize === 'small' ? 4 : 8}
            />
          ))}
        </Space>
      ),
    },
  ], [ballSize]);

  return (
    <div>
      <Card
        title="생성 히스토리"
        extra={
          <Space>
            <span>볼 크기:</span>
            <Radio.Group 
              value={ballSize} 
              onChange={handleBallSizeChange}
              size="small"
              buttonStyle="solid"
            >
              <Radio.Button value="small">작게</Radio.Button>
              <Radio.Button value="medium">보통</Radio.Button>
              <Radio.Button value="large">크게</Radio.Button>
            </Radio.Group>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={history}
          loading={loading}
          rowKey="setId"
          pagination={false}
        />
        <div style={{ marginTop: '16px', textAlign: 'right' }}>
          <Pagination
            current={page + 1}
            pageSize={size}
            total={total}
            onChange={(pageNum, pageSizeNum) => {
              setPage(pageNum - 1);
              setSize(pageSizeNum);
            }}
            showSizeChanger
            showTotal={(total) => `총 ${total}개`}
          />
        </div>
      </Card>
    </div>
  );
};
