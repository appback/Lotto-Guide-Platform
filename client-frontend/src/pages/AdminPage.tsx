import React, { useState, useEffect } from 'react';
import { Card, Upload, Button, message, Space, InputNumber, Typography, Divider, Alert, Input, Table, Tag, Select, Tabs, Form, Switch } from 'antd';
import { DownloadOutlined, SaveOutlined, SearchOutlined, InboxOutlined, EditOutlined, CloudDownloadOutlined, ReloadOutlined, StopOutlined } from '@ant-design/icons';
import type { UploadProps, UploadFile } from 'antd';
import { adminService, type StrategyDescription } from '@/services/adminService';
import type { DrawData, FetchAndSaveDrawResponse } from '@/services/adminService';

const { TextArea } = Input;
const { Dragger } = Upload;
const { Title, Text, Paragraph } = Typography;
const { TabPane } = Tabs;

export const AdminPage: React.FC = () => {
  return (
    <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
      <Card>
        <Title level={2}>관리자 페이지</Title>
        <Text type="secondary">로또 추첨 데이터 및 설정 관리</Text>

        <Divider />

        <Tabs defaultActiveKey="draws" size="large">
          <TabPane tab="로또당첨번호" key="draws">
            <DrawDataManager />
          </TabPane>
          <TabPane tab="전략 설명" key="strategies">
            <StrategyDescriptionManager />
          </TabPane>
          <TabPane tab="운명의 추천 미션" key="destiny-mission">
            <DestinyMissionManager />
          </TabPane>
          <TabPane tab="운명의 추천 경고" key="destiny-warning">
            <DestinyWarningManager />
          </TabPane>
          <TabPane tab="옵션 관리" key="options">
            <OptionsManager />
          </TabPane>
        </Tabs>
      </Card>
    </div>
  );
};

// 로또당첨번호 관리 컴포넌트 (통합 관리 레이어)
const DrawDataManager: React.FC = () => {
  const [uploading, setUploading] = useState(false);
  const [downloading, setDownloading] = useState(false);
  
  // CSV 업로드 설정
  const [csvFile, setCsvFile] = useState<UploadFile | null>(null);
  const [delimiter, setDelimiter] = useState<string>(',');
  
  // 수동 입력
  const [manualInput, setManualInput] = useState<string>('');
  const [saving, setSaving] = useState(false);
  
  // 조회
  const [searchDrawNo, setSearchDrawNo] = useState<number | null>(null);
  const [searching, setSearching] = useState(false);
  const [searchResult, setSearchResult] = useState<DrawData | null>(null);
  const [drawsList, setDrawsList] = useState<DrawData[]>([]);
  const [loadingDraws, setLoadingDraws] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState<number>(20);
  const [totalDraws, setTotalDraws] = useState(0);

  // CSV 파일 선택
  const handleFileChange: UploadProps['onChange'] = (info) => {
    const { fileList } = info;
    if (fileList.length > 0) {
      setCsvFile(fileList[0]);
    } else {
      setCsvFile(null);
    }
  };

  // CSV 데이터 로드
  const handleLoadCsv = async () => {
    if (!csvFile || !csvFile.originFileObj) {
      message.error('CSV 파일을 선택해주세요.');
      return;
    }

    setUploading(true);
    try {
      const result = await adminService.importCsv(csvFile.originFileObj, true, delimiter); // 헤더 포함 true 고정
      
      if (result.success) {
        message.success(
          `CSV 업로드 완료: 저장 ${result.savedCount}개, 건너뜀 ${result.skippedCount}개, 오류 ${result.errorCount}개`
        );
        setCsvFile(null);
        // 조회 목록 새로고침 (이미 조회된 경우)
        if (drawsList.length > 0) {
          loadDrawsList(currentPage - 1, pageSize);
        }
      } else {
        throw new Error(result.message);
      }
    } catch (error: any) {
      message.error(error.message || 'CSV 업로드 실패');
    } finally {
      setUploading(false);
    }
  };

  // CSV 파일 제거
  const handleRemoveFile = () => {
    setCsvFile(null);
  };

  // CSV 다운로드
  const handleDownload = async () => {
    setDownloading(true);
    try {
      const blob = await adminService.exportCsv();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `lotto_draws_${new Date().toISOString().split('T')[0]}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      message.success('CSV 다운로드 완료');
    } catch (error: any) {
      message.error(error.message || 'CSV 다운로드 실패');
    } finally {
      setDownloading(false);
    }
  };

  // 수동 저장/업데이트
  const handleSaveManual = async () => {
    if (!manualInput.trim()) {
      message.error('JSON 데이터를 입력해주세요.');
      return;
    }

    setSaving(true);
    try {
      const data = JSON.parse(manualInput);
      const result = await adminService.saveDraw(data);
      if (result.success) {
        message.success(result.message || `회차 ${result.drawNo} ${result.isUpdate ? '업데이트' : '저장'} 완료`);
        setManualInput(''); // 입력 초기화
        // 조회 목록 새로고침 (이미 조회된 경우)
        if (drawsList.length > 0) {
          loadDrawsList(currentPage - 1, pageSize);
        }
      } else {
        throw new Error(result.message);
      }
    } catch (error: any) {
      message.error(error.message || '저장 실패');
    } finally {
      setSaving(false);
    }
  };

  // 단일 회차 조회
  const handleSearchDraw = async () => {
    if (!searchDrawNo || searchDrawNo < 1) {
      message.error('회차 번호를 입력해주세요.');
      return;
    }

    setSearching(true);
    try {
      const result = await adminService.getDraw(searchDrawNo);
      if (result.success && result.data) {
        setSearchResult(result.data);
        message.success(`회차 ${searchDrawNo} 조회 완료`);
      } else {
        throw new Error(result.message || '조회 실패');
      }
    } catch (error: any) {
      message.error(error.message || '조회 실패');
      setSearchResult(null);
    } finally {
      setSearching(false);
    }
  };

  // 전체 목록 조회 (버튼 클릭 시에만 조회)
  const loadDrawsList = async (page: number = 0, size: number = pageSize) => {
    setLoadingDraws(true);
    try {
      const result = await adminService.getDraws(page, size);
      if (result.success && result.data) {
        setDrawsList(result.data);
        setTotalDraws(result.total || 0);
        setCurrentPage(page + 1);
      } else {
        throw new Error(result.message || '조회 실패');
      }
    } catch (error: any) {
      message.error(error.message || '목록 조회 실패');
    } finally {
      setLoadingDraws(false);
    }
  };

  // 페이지 크기 변경 핸들러
  const handlePageSizeChange = (newPageSize: number) => {
    setPageSize(newPageSize);
    setCurrentPage(1);
    loadDrawsList(0, newPageSize);
  };

  // 테이블 컬럼 정의
  const columns = [
    {
      title: '회차',
      dataIndex: 'drawNo',
      key: 'drawNo',
      width: 80,
      sorter: (a: DrawData, b: DrawData) => a.drawNo - b.drawNo,
    },
    {
      title: '추첨일',
      dataIndex: 'drawDate',
      key: 'drawDate',
      width: 120,
    },
    {
      title: '번호',
      key: 'numbers',
      render: (_: any, record: DrawData) => (
        <Space>
          {record.numbers.map((num, idx) => (
            <Tag key={idx} color="blue">{num}</Tag>
          ))}
          <Tag color="red">+{record.bonus}</Tag>
        </Space>
      ),
    },
    {
      title: '당첨금(억)',
      dataIndex: 'totalPrize',
      key: 'totalPrize',
      width: 100,
      render: (value: number) => value ? `${value}` : '-',
    },
    {
      title: '당첨인원',
      dataIndex: 'winnerCount',
      key: 'winnerCount',
      width: 100,
      render: (value: number) => value ? `${value}명` : '-',
    },
    {
      title: '인원당당첨금(억)',
      dataIndex: 'prizePerPerson',
      key: 'prizePerPerson',
      width: 120,
      render: (value: number) => value ? `${value}` : '-',
    },
  ];

  return (
    <div>
      {/* 1. 내려받기 버튼 */}
      <Card
        title="데이터 내려받기"
        style={{ marginBottom: '24px' }}
      >
        <Button
          icon={<DownloadOutlined />}
          onClick={handleDownload}
          loading={downloading}
          type="default"
        >
          모든 데이터 CSV 다운로드
        </Button>
      </Card>

      {/* 2. 파일 올리는 영역 */}
      <Card
        title="CSV 업로드"
        style={{ marginBottom: '24px' }}
      >
        <Alert
          message="CSV 형식"
          description={
            <div>
              <p>헤더: <code>drawNo,drawDate,n1,n2,n3,n4,n5,n6,bonus,totalPrize,winnerCount,prizePerPerson</code></p>
              <p>예시:</p>
              <pre style={{ background: '#f5f5f5', padding: '8px', borderRadius: '4px', textAlign: 'left' }}>
{`drawNo,drawDate,n1,n2,n3,n4,n5,n6,bonus,totalPrize,winnerCount,prizePerPerson
1,2002-12-07,10,23,29,33,37,40,16,280.3,15,18.7
2,2002-12-14,9,13,21,25,32,42,5,,`}
              </pre>
            </div>
          }
          type="info"
          style={{ marginBottom: '16px' }}
        />
        <Dragger
          accept=".csv"
          fileList={csvFile ? [csvFile] : []}
          onChange={handleFileChange}
          beforeUpload={() => false}
          multiple={false}
          disabled={uploading}
        >
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">CSV 파일을 클릭하거나 드래그하여 선택</p>
          <p className="ant-upload-hint">
            CSV 파일만 선택 가능합니다. 회차만 있고 내용이 없는 경우 저장되지 않습니다.
          </p>
        </Dragger>
        <div style={{ marginTop: '16px' }}>
          <Space direction="vertical" style={{ width: '100%' }}>
            <Space>
              <span>구분자:</span>
              <Select
                value={delimiter}
                onChange={setDelimiter}
                style={{ width: 150 }}
                options={[
                  { label: '쉼표 (,)', value: ',' },
                  { label: '탭 (\t)', value: '\t' },
                  { label: '세미콜론 (;)', value: ';' },
                ]}
              />
            </Space>
            <Space>
              <Button
                type="primary"
                onClick={handleLoadCsv}
                loading={uploading}
                disabled={!csvFile}
              >
                데이터 로드
              </Button>
              <Button
                onClick={handleRemoveFile}
                disabled={!csvFile || uploading}
              >
                파일 제거
              </Button>
            </Space>
          </Space>
        </div>
      </Card>

      {/* 수동 입력 */}
      <Card
        title="수동 입력 (JSON)"
        style={{ marginBottom: '24px' }}
      >
        <Alert
          message="JSON 형식"
          description={
            <div>
              <p>기존 데이터가 있어도 갱신됩니다.</p>
              <pre style={{ background: '#f5f5f5', padding: '8px', borderRadius: '4px', marginTop: '8px', textAlign: 'left' }}>
{`{
  "drawNo": 1206,
  "drawDate": "2026-01-11",
  "numbers": [1, 3, 17, 26, 27, 42],
  "bonus": 23,
  "totalPrize": 280.3,
  "winnerCount": 15,
  "prizePerPerson": 18.7
}`}
              </pre>
            </div>
          }
          type="info"
          style={{ marginBottom: '16px' }}
        />
        <Space direction="vertical" style={{ width: '100%' }}>
          <TextArea
            rows={8}
            value={manualInput}
            onChange={(e) => setManualInput(e.target.value)}
            placeholder="JSON 데이터를 입력하세요..."
            style={{ fontFamily: 'monospace', textAlign: 'left' }}
          />
          <Button
            icon={<SaveOutlined />}
            onClick={handleSaveManual}
            loading={saving}
            type="primary"
          >
            저장/업데이트
          </Button>
        </Space>
      </Card>

      {/* 3. 조회 테이블 (조회 버튼을 눌러야 조회) */}
      <Card
        title="조회"
        style={{ marginBottom: '24px' }}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          {/* 단일 회차 조회 */}
          <Card size="small" title="단일 회차 조회">
            <Space>
              <Text>회차 번호:</Text>
              <InputNumber
                min={1}
                value={searchDrawNo || undefined}
                onChange={(value) => setSearchDrawNo(value)}
                style={{ width: '120px' }}
                placeholder="회차 번호"
              />
              <Button
                icon={<SearchOutlined />}
                onClick={handleSearchDraw}
                loading={searching}
                type="primary"
              >
                조회
              </Button>
            </Space>
            {searchResult && (
              <div style={{ marginTop: '16px', padding: '12px', background: '#f5f5f5', borderRadius: '4px' }}>
                <p><strong>회차:</strong> {searchResult.drawNo}</p>
                <p><strong>추첨일:</strong> {searchResult.drawDate}</p>
                <p><strong>번호:</strong> {searchResult.numbers.join(', ')} + {searchResult.bonus}</p>
                {searchResult.totalPrize && <p><strong>당첨금:</strong> {searchResult.totalPrize}억</p>}
                {searchResult.winnerCount && <p><strong>당첨인원:</strong> {searchResult.winnerCount}명</p>}
                {searchResult.prizePerPerson && <p><strong>인당당첨금:</strong> {searchResult.prizePerPerson}억</p>}
              </div>
            )}
          </Card>

          {/* 전체 목록 조회 */}
          <Card size="small" title="전체 목록 조회">
            <Space style={{ marginBottom: '16px' }}>
              <Button
                onClick={() => loadDrawsList(0, pageSize)}
                loading={loadingDraws}
                type="primary"
              >
                조회
              </Button>
              <Text>페이지 크기:</Text>
              <Select
                value={pageSize}
                onChange={handlePageSizeChange}
                style={{ width: 100 }}
                options={[
                  { label: '20', value: 20 },
                  { label: '50', value: 50 },
                  { label: '100', value: 100 },
                ]}
              />
            </Space>
            {drawsList.length > 0 && (
              <Table
                columns={columns}
                dataSource={drawsList}
                rowKey="drawNo"
                loading={loadingDraws}
                pagination={{
                  current: currentPage,
                  pageSize: pageSize,
                  total: totalDraws,
                  onChange: (page) => loadDrawsList(page - 1, pageSize),
                  onShowSizeChange: (_current, size) => handlePageSizeChange(size),
                  showSizeChanger: true,
                  pageSizeOptions: ['20', '50', '100'],
                  showTotal: (total, range) => `${range[0]}-${range[1]} / 총 ${total}개`,
                }}
                size="small"
              />
            )}
          </Card>
        </Space>
      </Card>

      {/* 공공데이터 업로드 */}
      <Card
        title="공공데이터 업로드"
        style={{ marginBottom: '24px' }}
      >
        <PublicDataUploadSection />
      </Card>
    </div>
  );
};

// 전략 설명 관리 컴포넌트
const StrategyDescriptionManager: React.FC = () => {
  const [descriptions, setDescriptions] = useState<StrategyDescription[]>([]);
  const [loading, setLoading] = useState(false);
  const [editingStrategy, setEditingStrategy] = useState<string | null>(null);
  const [form] = Form.useForm();

  // 로컬 스토리지 키
  const STORAGE_KEY_STRATEGY_HASHES = 'lotto_strategy_content_hashes';

  // 로컬 스토리지에서 해시 정보 조회
  const getCachedHashes = (): Record<string, string> => {
    try {
      const cached = localStorage.getItem(STORAGE_KEY_STRATEGY_HASHES);
      return cached ? JSON.parse(cached) : {};
    } catch {
      return {};
    }
  };

  // 로컬 스토리지에 해시 정보 저장
  const saveCachedHashes = (hashes: Record<string, string>) => {
    try {
      localStorage.setItem(STORAGE_KEY_STRATEGY_HASHES, JSON.stringify(hashes));
    } catch (error) {
      console.warn('해시 정보 저장 실패:', error);
    }
  };

  // 전략 설명 로드 (해시 일련번호 비교)
  const loadDescriptions = async (forceReload: boolean = false) => {
    setLoading(true);
    try {
      const response = await adminService.getStrategyDescriptions();
      if (response.success && response.data) {
        const descriptions = response.data;
        const cachedHashes = getCachedHashes();
        const serverHashes: Record<string, string> = {};
        const needsUpdate: string[] = [];

        // 해시 일련번호 비교
        descriptions.forEach((desc) => {
          const serverHash = desc.contentHash || '';
          const cachedHash = cachedHashes[desc.strategyCode];
          
          serverHashes[desc.strategyCode] = serverHash;
          
          // 해시가 다르거나 강제 새로고침이면 갱신 필요
          if (forceReload || !cachedHash || cachedHash !== serverHash) {
            needsUpdate.push(desc.strategyCode);
          }
        });

        // 해시가 동일한 경우 불러오기 없음
        if (needsUpdate.length === 0 && !forceReload) {
          message.info('모든 전략 설명이 최신 상태입니다. 갱신할 내용이 없습니다.');
          return;
        }

        // 해시가 다른 경우 갱신 (내용이 변경됨)
        if (needsUpdate.length > 0 && !forceReload) {
          message.success(`${needsUpdate.length}개 전략 설명이 갱신되었습니다.`);
        }

        // 서버 데이터로 업데이트
        setDescriptions(descriptions);
        
        // 해시 정보 저장
        saveCachedHashes(serverHashes);
      } else {
        message.error(response.message || '전략 설명 조회 실패');
      }
    } catch (error: any) {
      message.error(error.message || '전략 설명 조회 실패');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDescriptions();
  }, []);

  // 전략 설명 수정
  const handleSave = async (strategyCode: string) => {
    try {
      const values = await form.validateFields();
      const request = {
        title: values.title,
        shortDescription: values.shortDescription,
        description: values.description,
        features: values.features.split('\n').filter((s: string) => s.trim().length > 0),
        algorithm: values.algorithm.split('\n').filter((s: string) => s.trim().length > 0),
        scenarios: values.scenarios.split('\n').filter((s: string) => s.trim().length > 0),
        notes: values.notes ? values.notes.split('\n').filter((s: string) => s.trim().length > 0) : undefined,
      };
      const response = await adminService.updateStrategyDescription(strategyCode, request);
      if (response.success) {
        message.success('전략 설명 수정 완료 (버전이 자동으로 증가했습니다)');
        setEditingStrategy(null);
        form.resetFields();
        loadDescriptions(true);
      } else {
        message.error(response.message || '전략 설명 수정 실패');
      }
    } catch (error: any) {
      if (error.errorFields) {
        return;
      }
      message.error(error.message || '전략 설명 수정 실패');
    }
  };

  // 편집 시작
  const handleEdit = (description: StrategyDescription) => {
    setEditingStrategy(description.strategyCode);
    form.setFieldsValue({
      title: description.title,
      shortDescription: description.shortDescription,
      description: description.description,
      features: description.features.join('\n'),
      algorithm: description.algorithm.join('\n'),
      scenarios: description.scenarios.join('\n'),
      notes: description.notes?.join('\n') || '',
    });
  };

  // 편집 취소
  const handleCancel = () => {
    setEditingStrategy(null);
    form.resetFields();
  };

  // CSV 다운로드
  const handleExportCsv = async () => {
    try {
      const blob = await adminService.exportStrategyDescriptionsCsv();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `strategy_descriptions_${new Date().toISOString().split('T')[0]}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      message.success('CSV 다운로드 완료');
    } catch (error: any) {
      message.error(error.message || 'CSV 다운로드 실패');
    }
  };

  // 전략 설명 CSV 업로드
  const [strategyCsvFile, setStrategyCsvFile] = useState<UploadFile | null>(null);
  const [strategyDelimiter, setStrategyDelimiter] = useState<string>(',');
  const [strategyUploading, setStrategyUploading] = useState(false);

  const handleStrategyFileChange: UploadProps['onChange'] = (info) => {
    const { fileList } = info;
    if (fileList.length > 0) {
      setStrategyCsvFile(fileList[0]);
    } else {
      setStrategyCsvFile(null);
    }
  };

  const handleImportStrategyCsv = async () => {
    if (!strategyCsvFile || !strategyCsvFile.originFileObj) {
      message.error('CSV 파일을 선택해주세요.');
      return;
    }

    setStrategyUploading(true);
    try {
      const result = await adminService.importStrategyDescriptionsCsv(strategyCsvFile.originFileObj, true, strategyDelimiter); // 헤더 포함 true 고정
      
      if (result.success) {
        message.success(
          `CSV 업로드 완료: 신규 ${result.savedCount}개, 업데이트 ${result.updatedCount}개, 오류 ${result.errorCount}개`
        );
        setStrategyCsvFile(null);
        loadDescriptions(true);
      } else {
        throw new Error(result.message);
      }
    } catch (error: any) {
      message.error(error.message || 'CSV 업로드 실패');
    } finally {
      setStrategyUploading(false);
    }
  };

  return (
    <div>
      <Space style={{ marginBottom: '16px' }}>
        <Button onClick={() => loadDescriptions(false)} loading={loading}>
          새로고침 (버전 확인)
        </Button>
        <Button onClick={() => loadDescriptions(true)} loading={loading}>
          강제 새로고침
        </Button>
        <Button icon={<DownloadOutlined />} onClick={handleExportCsv}>
          CSV 다운로드
        </Button>
      </Space>

      {/* CSV 업로드 섹션 */}
      <Card size="small" title="CSV 업로드" style={{ marginBottom: '16px' }}>
        <Alert
          message="CSV 형식"
          description={
            <div>
              <p>헤더: <code>strategyCode,title,shortDescription,description,features,algorithm,scenarios,notes,contentHash</code></p>
              <p style={{ fontSize: '12px', color: '#666' }}>
                <strong>참고:</strong> contentHash는 내용 기반 해시 일련번호입니다. 내용이 변경되면 자동으로 생성되며, CSV에서 생략 가능합니다.
              </p>
              <p>배열 필드(features, algorithm, scenarios, notes)는 파이프(|)로 구분합니다.</p>
            </div>
          }
          type="info"
          style={{ marginBottom: '16px' }}
        />
        <Dragger
          accept=".csv"
          fileList={strategyCsvFile ? [strategyCsvFile] : []}
          onChange={handleStrategyFileChange}
          beforeUpload={() => false}
          multiple={false}
          disabled={strategyUploading}
        >
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">CSV 파일을 클릭하거나 드래그하여 선택</p>
        </Dragger>
        <div style={{ marginTop: '16px' }}>
          <Space direction="vertical" style={{ width: '100%' }}>
            <Space>
              <span>구분자:</span>
              <Select
                value={strategyDelimiter}
                onChange={setStrategyDelimiter}
                style={{ width: 150 }}
                options={[
                  { label: '쉼표 (,)', value: ',' },
                  { label: '탭 (\t)', value: '\t' },
                  { label: '세미콜론 (;)', value: ';' },
                ]}
              />
            </Space>
            <Space>
              <Button
                type="primary"
                onClick={handleImportStrategyCsv}
                loading={strategyUploading}
                disabled={!strategyCsvFile}
              >
                CSV 업로드
              </Button>
              <Button
                onClick={() => setStrategyCsvFile(null)}
                disabled={!strategyCsvFile || strategyUploading}
              >
                파일 제거
              </Button>
            </Space>
          </Space>
        </div>
      </Card>

      <Tabs defaultActiveKey={descriptions[0]?.strategyCode || 'FREQUENT_TOP'}>
        {descriptions.map((desc) => (
          <TabPane tab={desc.strategyCode} key={desc.strategyCode}>
            {editingStrategy === desc.strategyCode ? (
              <Form form={form} layout="vertical">
                <Form.Item label="제목" name="title" rules={[{ required: true, message: '제목을 입력해주세요' }]}>
                  <Input />
                </Form.Item>
                <Form.Item label="간단 설명" name="shortDescription" rules={[{ required: true, message: '간단 설명을 입력해주세요' }]}>
                  <TextArea rows={2} />
                </Form.Item>
                <Form.Item label="상세 설명" name="description" rules={[{ required: true, message: '상세 설명을 입력해주세요' }]}>
                  <TextArea rows={3} />
                </Form.Item>
                <Form.Item label="특징 (줄바꿈으로 구분)" name="features" rules={[{ required: true, message: '특징을 입력해주세요' }]}>
                  <TextArea rows={5} placeholder="각 특징을 줄바꿈으로 구분하여 입력하세요" />
                </Form.Item>
                <Form.Item label="알고리즘 (줄바꿈으로 구분)" name="algorithm" rules={[{ required: true, message: '알고리즘을 입력해주세요' }]}>
                  <TextArea rows={6} placeholder="각 단계를 줄바꿈으로 구분하여 입력하세요" />
                </Form.Item>
                <Form.Item label="적용 시나리오 (줄바꿈으로 구분)" name="scenarios" rules={[{ required: true, message: '적용 시나리오를 입력해주세요' }]}>
                  <TextArea rows={4} placeholder="각 시나리오를 줄바꿈으로 구분하여 입력하세요" />
                </Form.Item>
                <Form.Item label="주의사항 (줄바꿈으로 구분, 선택적)" name="notes">
                  <TextArea rows={4} placeholder="각 주의사항을 줄바꿈으로 구분하여 입력하세요 (선택적)" />
                </Form.Item>
                <Space>
                  <Button type="primary" onClick={() => handleSave(desc.strategyCode)}>
                    저장
                  </Button>
                  <Button onClick={handleCancel}>취소</Button>
                </Space>
              </Form>
            ) : (
              <div>
                <Space style={{ marginBottom: '16px' }}>
                  <Button icon={<EditOutlined />} onClick={() => handleEdit(desc)}>
                    수정
                  </Button>
                </Space>
                <div>
                  <Title level={4}>{desc.title}</Title>
                  <Paragraph><Text strong>간단 설명:</Text> {desc.shortDescription}</Paragraph>
                  <Paragraph><Text strong>상세 설명:</Text> {desc.description}</Paragraph>
                  <Divider />
                  <Title level={5}>특징</Title>
                  <ul>
                    {desc.features.map((feature, index) => (
                      <li key={index}>{feature}</li>
                    ))}
                  </ul>
                  <Divider />
                  <Title level={5}>알고리즘</Title>
                  <ol>
                    {desc.algorithm.map((step, index) => (
                      <li key={index}>{step}</li>
                    ))}
                  </ol>
                  <Divider />
                  <Title level={5}>적용 시나리오</Title>
                  <ul>
                    {desc.scenarios.map((scenario, index) => (
                      <li key={index}>{scenario}</li>
                    ))}
                  </ul>
                  {desc.notes && desc.notes.length > 0 && (
                    <>
                      <Divider />
                      <Title level={5}>주의사항</Title>
                      <ul>
                        {desc.notes.map((note, index) => (
                          <li key={index} style={{ color: '#ff4d4f' }}>{note}</li>
                        ))}
                      </ul>
                    </>
                  )}
                </div>
              </div>
            )}
          </TabPane>
        ))}
      </Tabs>
    </div>
  );
};

// 운명의 추천 미션 관리 컴포넌트 (A/B/C 멘트 관리)
const DestinyMissionManager: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'A' | 'B' | 'C'>('A');

  return (
    <div>
      <Tabs activeKey={activeTab} onChange={(key) => setActiveTab(key as 'A' | 'B' | 'C')}>
        <TabPane tab="A 멘트 (해석/은유)" key="A">
          <MissionPhraseManager type="A" />
        </TabPane>
        <TabPane tab="B 멘트 (행동/장소/색감)" key="B">
          <MissionPhraseManager type="B" />
        </TabPane>
        <TabPane tab="C 멘트 (추천도/마무리)" key="C">
          <MissionPhraseManager type="C" />
        </TabPane>
      </Tabs>
    </div>
  );
};

// A/B/C 멘트 관리 컴포넌트 (공통)
interface MissionPhraseManagerProps {
  type: 'A' | 'B' | 'C';
}

const MissionPhraseManager: React.FC<MissionPhraseManagerProps> = ({ type }) => {
  const [csvFile, setCsvFile] = useState<UploadFile | null>(null);
  const [uploading, setUploading] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [phrasesList, setPhrasesList] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState<number>(20);
  const [total, setTotal] = useState(0);

  const handleFileChange: UploadProps['onChange'] = (info) => {
    const { fileList } = info;
    if (fileList.length > 0) {
      setCsvFile(fileList[0]);
    } else {
      setCsvFile(null);
    }
  };

  const handleExportCsv = async () => {
    setDownloading(true);
    try {
      let blob: Blob;
      let filename: string;
      
      if (type === 'A') {
        blob = await adminService.exportMissionPhraseACsv();
        filename = `mission_phrase_a_${new Date().toISOString().split('T')[0]}.csv`;
      } else if (type === 'B') {
        blob = await adminService.exportMissionPhraseBCsv();
        filename = `mission_phrase_b_${new Date().toISOString().split('T')[0]}.csv`;
      } else {
        blob = await adminService.exportMissionPhraseCCsv();
        filename = `mission_phrase_c_${new Date().toISOString().split('T')[0]}.csv`;
      }
      
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      message.success('CSV 다운로드 완료');
    } catch (error: any) {
      message.error(error.message || 'CSV 다운로드 실패');
    } finally {
      setDownloading(false);
    }
  };

  const handleImportCsv = async () => {
    if (!csvFile || !csvFile.originFileObj) {
      message.error('CSV 파일을 선택해주세요.');
      return;
    }

    setUploading(true);
    try {
      let result;
      
      if (type === 'A') {
        result = await adminService.importMissionPhraseACsv(csvFile.originFileObj);
      } else if (type === 'B') {
        result = await adminService.importMissionPhraseBCsv(csvFile.originFileObj);
      } else {
        result = await adminService.importMissionPhraseCCsv(csvFile.originFileObj);
      }
      
      if (result.success) {
        const errorMsg = result.errorCount > 0 
          ? ` 저장 ${result.savedCount}개, 오류 ${result.errorCount}개`
          : ` 저장 ${result.savedCount}개`;
        
        if (result.errorCount > 0 && result.errors && result.errors.length > 0) {
          // 에러가 있으면 경고 메시지와 함께 상세 에러 표시
          message.warning(`CSV 업로드 완료:${errorMsg}`);
          console.error('CSV 업로드 에러 상세:', result.errors);
          // 에러 목록을 모달이나 Alert로 표시할 수도 있음
        } else {
          message.success(`CSV 업로드 완료:${errorMsg}`);
        }
        
        setCsvFile(null);
        // 업로드 후 자동으로 조회 (저장된 데이터가 있으면)
        if (result.savedCount > 0) {
          loadList(0, pageSize);
        } else {
          // 저장된 데이터가 없으면 경고
          message.warning('저장된 데이터가 없습니다. CSV 형식을 확인해주세요.');
        }
      } else {
        throw new Error(result.message);
      }
    } catch (error: any) {
      message.error(error.message || 'CSV 업로드 실패');
    } finally {
      setUploading(false);
    }
  };

  const loadList = async (page: number = 0, size: number = pageSize) => {
    setLoading(true);
    try {
      let result;
      
      if (type === 'A') {
        result = await adminService.getMissionPhraseA(page, size);
      } else if (type === 'B') {
        result = await adminService.getMissionPhraseB(page, size);
      } else {
        result = await adminService.getMissionPhraseC(page, size);
      }
      
      if (result.success && result.data) {
        setPhrasesList(result.data);
        setTotal(result.total || 0);
        setCurrentPage(page + 1);
      } else {
        throw new Error(result.message || '조회 실패');
      }
    } catch (error: any) {
      message.error(error.message || '목록 조회 실패');
    } finally {
      setLoading(false);
    }
  };

  const handlePageSizeChange = (newPageSize: number) => {
    setPageSize(newPageSize);
    setCurrentPage(1);
    loadList(0, newPageSize);
  };

  // 컬럼 정의
  const getColumns = () => {
    const baseColumns = [
      {
        title: 'ID',
        dataIndex: 'id',
        key: 'id',
        width: 80,
      },
      {
        title: '텍스트',
        dataIndex: 'text',
        key: 'text',
        ellipsis: true,
      },
      {
        title: '가중치',
        dataIndex: 'weightBase',
        key: 'weightBase',
        width: 80,
      },
    ];

    if (type === 'A') {
      return [
        ...baseColumns,
        {
          title: '전략 태그',
          dataIndex: 'strategyTags',
          key: 'strategyTags',
          width: 150,
          render: (value: string) => value || '-',
        },
        {
          title: '조합 태그',
          dataIndex: 'comboTags',
          key: 'comboTags',
          width: 150,
          render: (value: string) => value || '-',
        },
        {
          title: '별자리 태그',
          dataIndex: 'zodiacTags',
          key: 'zodiacTags',
          width: 120,
          render: (value: string) => value || '-',
        },
        {
          title: '톤 태그',
          dataIndex: 'toneTags',
          key: 'toneTags',
          width: 100,
          render: (value: string) => value || '-',
        },
      ];
    } else if (type === 'B') {
      return [
        ...baseColumns,
        {
          title: '장소 힌트',
          dataIndex: 'placeHint',
          key: 'placeHint',
          width: 120,
          render: (value: string) => value || '-',
        },
        {
          title: '색감 힌트',
          dataIndex: 'colorHint',
          key: 'colorHint',
          width: 120,
          render: (value: string) => value || '-',
        },
        {
          title: '정렬 태그',
          dataIndex: 'alignTags',
          key: 'alignTags',
          width: 150,
          render: (value: string) => value || '-',
        },
        {
          title: '회피 태그',
          dataIndex: 'avoidTags',
          key: 'avoidTags',
          width: 150,
          render: (value: string) => value || '-',
        },
      ];
    } else {
      return [
        ...baseColumns,
        {
          title: '톤 태그',
          dataIndex: 'toneTags',
          key: 'toneTags',
          width: 100,
          render: (value: string) => value || '-',
        },
      ];
    }
  };

  // CSV 형식 설명
  const getCsvFormat = () => {
    if (type === 'A') {
      return {
        header: 'id,text,strategy_tags,combo_tags,zodiac_tags,tone_tags,weight_base',
        example: `id,text,strategy_tags,combo_tags,zodiac_tags,tone_tags,weight_base
1,"물의 기운이 잔잔하게 깔려 있어요.","[\"FREQUENT_TOP\",\"BALANCED\"]","[\"ODD_HEAVY\",\"SUM_MID\"]","[\"PISCES\",\"CANCER\"]","[\"TAROT\"]",1`,
      };
    } else if (type === 'B') {
      return {
        header: 'id,text,place_hint,color_hint,align_tags,avoid_tags,weight_base',
        example: `id,text,place_hint,color_hint,align_tags,avoid_tags,weight_base
1,"오늘은 강가나 물이 보이는 곳을 지나칠 때 가볍게 선택해보세요.",RIVER,BLUE,"[\"WATER\",\"GENTLE\"]","[\"FIRE\",\"BOLD\"]",1`,
      };
    } else {
      return {
        header: 'id,text,tone_tags,weight_base',
        example: `id,text,tone_tags,weight_base
1,"부담 없이, 오늘의 흐름만 챙기면 충분합니다.","[\"TAROT\"]",1`,
      };
    }
  };

  const csvFormat = getCsvFormat();

  return (
    <div>
      {/* 1. 내려받기 버튼 */}
      <Card title="데이터 내려받기" style={{ marginBottom: '24px' }}>
        <Button
          icon={<DownloadOutlined />}
          onClick={handleExportCsv}
          loading={downloading}
          type="default"
        >
          모든 데이터 CSV 다운로드
        </Button>
      </Card>

      {/* 2. 파일 올리는 영역 */}
      <Card title="CSV 업로드" style={{ marginBottom: '24px' }}>
        <Alert
          message="CSV 형식"
          description={
            <div>
              <p>헤더: <code>{csvFormat.header}</code></p>
              <p>예시:</p>
              <pre style={{ background: '#f5f5f5', padding: '8px', borderRadius: '4px', textAlign: 'left', fontSize: '12px' }}>
                {csvFormat.example}
              </pre>
              <p style={{ fontSize: '12px', color: '#666', marginTop: '8px' }}>
                <strong>참고:</strong> 전체 교체 모드이므로 업로드 시 기존 데이터는 모두 삭제되고 업로드된 데이터로 교체됩니다.
                <br />
                태그 필드는 JSON 배열 형식입니다 (예: <code>["TAG1","TAG2"]</code>).
              </p>
            </div>
          }
          type="info"
          style={{ marginBottom: '16px' }}
        />
        <Dragger
          accept=".csv"
          fileList={csvFile ? [csvFile] : []}
          onChange={handleFileChange}
          beforeUpload={() => false}
          multiple={false}
          disabled={uploading}
        >
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">CSV 파일을 클릭하거나 드래그하여 선택</p>
        </Dragger>
        <div style={{ marginTop: '16px' }}>
          <Space>
            <Button
              type="primary"
              onClick={handleImportCsv}
              loading={uploading}
              disabled={!csvFile}
            >
              데이터 로드
            </Button>
            <Button
              onClick={() => setCsvFile(null)}
              disabled={!csvFile || uploading}
            >
              파일 제거
            </Button>
          </Space>
        </div>
      </Card>

      {/* 3. 조회 테이블 */}
      <Card title="조회" style={{ marginBottom: '24px' }}>
        <Space style={{ marginBottom: '16px' }}>
          <Button
            onClick={() => loadList(0, pageSize)}
            loading={loading}
            type="primary"
          >
            조회
          </Button>
          <Text>페이지 크기:</Text>
          <Select
            value={pageSize}
            onChange={handlePageSizeChange}
            style={{ width: 100 }}
            options={[
              { label: '20', value: 20 },
              { label: '50', value: 50 },
              { label: '100', value: 100 },
            ]}
          />
        </Space>
        {loading && phrasesList.length === 0 && (
          <div style={{ textAlign: 'center', padding: '40px' }}>
            <Text type="secondary">조회 중...</Text>
          </div>
        )}
        {!loading && phrasesList.length === 0 && total === 0 && (
          <div style={{ textAlign: 'center', padding: '40px' }}>
            <Text type="secondary">저장된 데이터가 없습니다. CSV를 업로드해주세요.</Text>
          </div>
        )}
        {phrasesList.length > 0 && (
          <Table
            columns={getColumns()}
            dataSource={phrasesList}
            rowKey="id"
            loading={loading}
            pagination={{
              current: currentPage,
              pageSize: pageSize,
              total: total,
              onChange: (page) => loadList(page - 1, pageSize),
              onShowSizeChange: (_current, size) => handlePageSizeChange(size),
              showSizeChanger: true,
              pageSizeOptions: ['20', '50', '100'],
              showTotal: (total, range) => `${range[0]}-${range[1]} / 총 ${total}개`,
            }}
            size="small"
          />
        )}
      </Card>
    </div>
  );
};

// 운명의 추천 경고 관리 컴포넌트 (통합 관리 레이어)
const DestinyWarningManager: React.FC = () => {
  const [destinyMessageCsvFile, setDestinyMessageCsvFile] = useState<UploadFile | null>(null);
  const [destinyMessageUploading, setDestinyMessageUploading] = useState(false);
  const [destinyMessageDownloading, setDestinyMessageDownloading] = useState(false);
  const [messagesList, setMessagesList] = useState<any[]>([]);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState<number>(20);
  const [totalMessages, setTotalMessages] = useState(0);

  const handleDestinyMessageFileChange: UploadProps['onChange'] = (info) => {
    const { fileList } = info;
    if (fileList.length > 0) {
      setDestinyMessageCsvFile(fileList[0]);
    } else {
      setDestinyMessageCsvFile(null);
    }
  };

  const handleExportDestinyMessagesCsv = async () => {
    setDestinyMessageDownloading(true);
    try {
      const blob = await adminService.exportDestinyLimitMessagesCsv();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `destiny_limit_messages_${new Date().toISOString().split('T')[0]}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      message.success('CSV 다운로드 완료');
    } catch (error: any) {
      message.error(error.message || 'CSV 다운로드 실패');
    } finally {
      setDestinyMessageDownloading(false);
    }
  };

  const handleImportDestinyMessagesCsv = async () => {
    if (!destinyMessageCsvFile || !destinyMessageCsvFile.originFileObj) {
      message.error('CSV 파일을 선택해주세요.');
      return;
    }

    setDestinyMessageUploading(true);
    try {
      const result = await adminService.importDestinyLimitMessagesCsv(destinyMessageCsvFile.originFileObj);
      
      if (result.success) {
        const errorMsg = result.errorCount > 0 
          ? ` 저장 ${result.savedCount}개, 오류 ${result.errorCount}개`
          : ` 저장 ${result.savedCount}개`;
        
        if (result.errorCount > 0 && result.errors && result.errors.length > 0) {
          message.warning(`CSV 업로드 완료:${errorMsg}`);
          console.error('CSV 업로드 에러 상세:', result.errors);
        } else {
          message.success(`CSV 업로드 완료:${errorMsg}`);
        }
        
        setDestinyMessageCsvFile(null);
        // 업로드 후 자동으로 조회 (저장된 데이터가 있으면)
        if (result.savedCount > 0) {
          loadMessagesList(0, pageSize);
        } else {
          message.warning('저장된 데이터가 없습니다. CSV 형식을 확인해주세요.');
        }
      } else {
        throw new Error(result.message);
      }
    } catch (error: any) {
      message.error(error.message || 'CSV 업로드 실패');
    } finally {
      setDestinyMessageUploading(false);
    }
  };

  // 전체 목록 조회 (버튼 클릭 시에만 조회)
  const loadMessagesList = async (page: number = 0, size: number = pageSize) => {
    setLoadingMessages(true);
    try {
      const result = await adminService.getDestinyLimitMessages();
      if (result.success && result.data) {
        // 페이징 처리
        const total = result.data.length;
        const start = page * size;
        const end = Math.min(start + size, total);
        const pagedMessages = start < total ? result.data.slice(start, end) : [];
        
        setMessagesList(pagedMessages);
        setTotalMessages(total);
        setCurrentPage(page + 1);
      } else {
        throw new Error(result.message || '조회 실패');
      }
    } catch (error: any) {
      message.error(error.message || '목록 조회 실패');
    } finally {
      setLoadingMessages(false);
    }
  };

  // 페이지 크기 변경 핸들러
  const handlePageSizeChange = (newPageSize: number) => {
    setPageSize(newPageSize);
    setCurrentPage(1);
    loadMessagesList(0, newPageSize);
  };

  // 테이블 컬럼 정의
  const messageColumns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '순서',
      dataIndex: 'orderIndex',
      key: 'orderIndex',
      width: 80,
      sorter: (a: any, b: any) => a.orderIndex - b.orderIndex,
    },
    {
      title: '전체 메시지',
      dataIndex: 'message',
      key: 'message',
      ellipsis: true,
    },
    {
      title: 'A 문구',
      dataIndex: 'messagePartA',
      key: 'messagePartA',
      width: 200,
      ellipsis: true,
      render: (value: string) => value || '-',
    },
    {
      title: 'B 문구',
      dataIndex: 'messagePartB',
      key: 'messagePartB',
      width: 200,
      ellipsis: true,
      render: (value: string) => value || '-',
    },
  ];

  return (
    <div>
      {/* 1. 내려받기 버튼 */}
      <Card
        title="데이터 내려받기"
        style={{ marginBottom: '24px' }}
      >
        <Button
          icon={<DownloadOutlined />}
          onClick={handleExportDestinyMessagesCsv}
          loading={destinyMessageDownloading}
          type="default"
        >
          모든 데이터 CSV 다운로드
        </Button>
      </Card>

      {/* 2. 파일 올리는 영역 */}
      <Card
        title="CSV 업로드"
        style={{ marginBottom: '24px' }}
      >
        <Alert
          message="CSV 형식"
          description={
            <div>
              <p>헤더: <code>id,message,orderIndex</code></p>
              <p>예시:</p>
              <pre style={{ background: '#f5f5f5', padding: '8px', borderRadius: '4px', textAlign: 'left' }}>
{`id,message,orderIndex
1,"별들의 계시가 모두 전달되었습니다. 내일 다시 당신의 운명을 확인하세요.",1`}
              </pre>
              <p style={{ fontSize: '12px', color: '#666', marginTop: '8px' }}>
                <strong>참고:</strong> 전체 교체 모드이므로 업로드 시 기존 데이터는 모두 삭제되고 업로드된 데이터로 교체됩니다.
                <br />
                업로드 시 <code>message</code>를 마침표 기준으로 자동 분리하여 <code>messagePartA</code>와 <code>messagePartB</code>로 저장합니다.
              </p>
            </div>
          }
          type="info"
          style={{ marginBottom: '16px' }}
        />
        <Dragger
          accept=".csv"
          fileList={destinyMessageCsvFile ? [destinyMessageCsvFile] : []}
          onChange={handleDestinyMessageFileChange}
          beforeUpload={() => false}
          multiple={false}
          disabled={destinyMessageUploading}
        >
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">CSV 파일을 클릭하거나 드래그하여 선택</p>
        </Dragger>
        <div style={{ marginTop: '16px' }}>
          <Space>
            <Button
              type="primary"
              onClick={handleImportDestinyMessagesCsv}
              loading={destinyMessageUploading}
              disabled={!destinyMessageCsvFile}
            >
              데이터 로드
            </Button>
            <Button
              onClick={() => setDestinyMessageCsvFile(null)}
              disabled={!destinyMessageCsvFile || destinyMessageUploading}
            >
              파일 제거
            </Button>
          </Space>
        </div>
      </Card>

      {/* 3. 조회 테이블 (조회 버튼을 눌러야 조회) */}
      <Card
        title="조회"
        style={{ marginBottom: '24px' }}
      >
        <Space style={{ marginBottom: '16px' }}>
          <Button
            onClick={() => loadMessagesList(0, pageSize)}
            loading={loadingMessages}
            type="primary"
          >
            조회
          </Button>
          <Text>페이지 크기:</Text>
          <Select
            value={pageSize}
            onChange={handlePageSizeChange}
            style={{ width: 100 }}
            options={[
              { label: '20', value: 20 },
              { label: '50', value: 50 },
              { label: '100', value: 100 },
            ]}
          />
        </Space>
        {loadingMessages && messagesList.length === 0 && (
          <div style={{ textAlign: 'center', padding: '40px' }}>
            <Text type="secondary">조회 중...</Text>
          </div>
        )}
        {!loadingMessages && messagesList.length === 0 && totalMessages === 0 && (
          <div style={{ textAlign: 'center', padding: '40px' }}>
            <Text type="secondary">저장된 데이터가 없습니다. CSV를 업로드해주세요.</Text>
          </div>
        )}
        {messagesList.length > 0 && (
          <Table
            columns={messageColumns}
            dataSource={messagesList}
            rowKey="id"
            loading={loadingMessages}
            pagination={{
              current: currentPage,
              pageSize: pageSize,
              total: totalMessages,
              onChange: (page) => loadMessagesList(page - 1, pageSize),
              onShowSizeChange: (_current, size) => handlePageSizeChange(size),
              showSizeChanger: true,
              pageSizeOptions: ['20', '50', '100'],
              showTotal: (total, range) => `${range[0]}-${range[1]} / 총 ${total}개`,
            }}
            size="small"
          />
        )}
      </Card>
    </div>
  );
};

// 옵션 관리 컴포넌트
const OptionsManager: React.FC = () => {
  const [siteTitle, setSiteTitle] = useState<string>('로또 가이드');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  // 로컬 스토리지 키
  const STORAGE_KEY_SYSTEM_OPTIONS = 'lotto_system_options';
  const STORAGE_KEY_SYSTEM_OPTIONS_SERIAL = 'lotto_system_options_serial';

  // 로컬 스토리지에서 옵션 조회
  const getCachedOptions = (): Record<string, string> => {
    try {
      const cached = localStorage.getItem(STORAGE_KEY_SYSTEM_OPTIONS);
      return cached ? JSON.parse(cached) : {};
    } catch {
      return {};
    }
  };

  // 로컬 스토리지에 옵션 저장
  const saveCachedOptions = (options: Record<string, string>, serialNumber: string) => {
    try {
      localStorage.setItem(STORAGE_KEY_SYSTEM_OPTIONS, JSON.stringify(options));
      localStorage.setItem(STORAGE_KEY_SYSTEM_OPTIONS_SERIAL, serialNumber);
    } catch (error) {
      console.warn('옵션 정보 저장 실패:', error);
    }
  };

  // 시스템 옵션 로드 (일련번호 비교)
  const loadSystemOptions = async (forceReload: boolean = false) => {
    setLoading(true);
    try {
      // 로컬 스토리지에서 먼저 읽기
      const cachedOptions = getCachedOptions();
      const cachedSerial = localStorage.getItem(STORAGE_KEY_SYSTEM_OPTIONS_SERIAL);
      
      if (cachedOptions.site_title && !forceReload) {
        setSiteTitle(cachedOptions.site_title);
        form.setFieldsValue({ siteTitle: cachedOptions.site_title });
      }

      // 서버에서 옵션 조회
      const result = await adminService.getSystemOptions();
      
      if (result.success && result.data && result.serialNumber) {
        const serverSerial = result.serialNumber;
        
        // 일련번호 비교
        if (forceReload || !cachedSerial || cachedSerial !== serverSerial) {
          // 일련번호가 다르면 갱신
          const options = result.data;
          saveCachedOptions(options, serverSerial);
          
          if (options.site_title) {
            setSiteTitle(options.site_title);
            form.setFieldsValue({ siteTitle: options.site_title });
          } else {
            form.setFieldsValue({ siteTitle: '로또 가이드' });
          }
          
          if (!forceReload && cachedSerial && cachedSerial !== serverSerial) {
            message.success('시스템 옵션이 갱신되었습니다.');
          }
        }
        // 일련번호가 같으면 로컬 스토리지 값 사용 (이미 설정됨)
      } else {
        // 기본값 설정
        if (!cachedOptions.site_title) {
          form.setFieldsValue({ siteTitle: '로또 가이드' });
        }
      }
    } catch (error: any) {
      message.error(error.message || '시스템 옵션 조회 실패');
      // 로컬 스토리지에서 다시 시도
      const cachedOptions = getCachedOptions();
      if (cachedOptions.site_title) {
        setSiteTitle(cachedOptions.site_title);
        form.setFieldsValue({ siteTitle: cachedOptions.site_title });
      } else {
        form.setFieldsValue({ siteTitle: '로또 가이드' });
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSystemOptions();
  }, []);

  // 시스템 옵션 저장
  const handleSaveSiteTitle = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);
      
      const result = await adminService.updateSystemOption(
        'site_title',
        values.siteTitle,
        '웹사이트 브라우저 타이틀'
      );
      
      if (result.success) {
        message.success('웹사이트 타이틀 저장 완료');
        setSiteTitle(values.siteTitle);
        // 페이지 타이틀 즉시 업데이트
        document.title = values.siteTitle;
        // 로컬 스토리지 갱신 (일련번호가 변경되었으므로)
        loadSystemOptions(true);
      } else {
        throw new Error(result.message);
      }
    } catch (error: any) {
      if (error.errorFields) {
        return;
      }
      message.error(error.message || '저장 실패');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <Card title="시스템 옵션 관리">
        <Form form={form} layout="vertical">
          <Card size="small" title="웹사이트 타이틀" style={{ marginBottom: '24px' }}>
            <Alert
              message="웹사이트 타이틀 설정"
              description="브라우저 탭에 표시되는 웹사이트 타이틀을 설정합니다. 변경 사항은 저장 즉시 반영됩니다."
              type="info"
              style={{ marginBottom: '16px' }}
            />
            <Form.Item
              label="타이틀"
              name="siteTitle"
              rules={[{ required: true, message: '타이틀을 입력해주세요' }]}
            >
              <Input
                placeholder="예: 로또 가이드"
                maxLength={100}
                showCount
              />
            </Form.Item>
            <Space>
              <Button
                type="primary"
                onClick={handleSaveSiteTitle}
                loading={saving}
              >
                저장
              </Button>
              <Button onClick={() => loadSystemOptions(false)} loading={loading}>
                새로고침 (버전 확인)
              </Button>
              <Button onClick={() => loadSystemOptions(true)} loading={loading}>
                강제 새로고침
              </Button>
            </Space>
            {siteTitle && (
              <div style={{ marginTop: '16px', padding: '12px', background: '#f5f5f5', borderRadius: '4px' }}>
                <Text strong>현재 타이틀:</Text> {siteTitle}
              </div>
            )}
          </Card>
        </Form>
      </Card>
    </div>
  );
};

// 공공데이터 업로드 섹션 컴포넌트
const PublicDataUploadSection: React.FC = () => {
  const [drawNo, setDrawNo] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<FetchAndSaveDrawResponse | null>(null);

  const handleFetchAndSave = async () => {
    if (!drawNo || drawNo < 1) {
      message.error('회차 번호를 입력해주세요. (1 이상)');
      return;
    }

    setLoading(true);
    setResult(null);
    try {
      const response = await adminService.fetchAndSaveDraw(drawNo);
      setResult(response);
      
      if (response.success && response.fetched && response.saved) {
        message.success(
          response.isUpdate 
            ? `회차 ${drawNo} 조회 및 업데이트 완료` 
            : `회차 ${drawNo} 조회 및 저장 완료`
        );
      } else if (response.success && response.fetched && !response.saved) {
        message.warning(`회차 ${drawNo} 조회 성공, 저장 실패: ${response.message}`);
      } else {
        message.error(`회차 ${drawNo} 조회 실패: ${response.message}`);
      }
    } catch (error: any) {
      message.error(error.message || '조회 및 저장 실패');
      setResult({
        success: false,
        message: error.message || '조회 및 저장 실패',
        drawNo: drawNo,
        fetched: false,
        saved: false,
      });
    } finally {
      setLoading(false);
    }
  };

  const [refreshLoading, setRefreshLoading] = useState(false);
  const [refreshResult, setRefreshResult] = useState<any>(null);
  const [forceUpdate, setForceUpdate] = useState(false);

  const handleRefreshAll = async () => {
    setRefreshLoading(true);
    setRefreshResult(null);
    try {
      const response = await adminService.refreshData(forceUpdate);
      setRefreshResult(response);
      
      if (response.success) {
        message.success(
          `모든 회차 정보 가져오기 완료: 저장 ${response.savedCount}개, 실패 ${response.failedCount}개 (최신 회차: ${response.latestDrawNo})`
        );
      } else {
        message.error(`모든 회차 정보 가져오기 실패: ${response.message}`);
      }
    } catch (error: any) {
      message.error(error.message || '모든 회차 정보 가져오기 실패');
      setRefreshResult({
        success: false,
        message: error.message || '모든 회차 정보 가져오기 실패',
      });
    } finally {
      setRefreshLoading(false);
    }
  };

  const handleCancelRefresh = async () => {
    try {
      const response = await adminService.cancelRefreshData();
      if (response.success) {
        message.info(response.message);
      } else {
        message.error(`중단 요청 실패: ${response.message}`);
      }
    } catch (error: any) {
      message.error(error.message || '중단 요청 실패');
    }
  };

  return (
    <div>
      <Alert
        message="동행복권 API를 통한 회차별 데이터 조회 및 저장"
        description="회차 번호를 입력하고 '얻어오기' 버튼을 클릭하면 동행복권 API에서 해당 회차의 데이터를 조회하고, 성공 시 자동으로 DB에 저장합니다."
        type="info"
        style={{ marginBottom: '16px' }}
      />
      
      <Space direction="vertical" style={{ width: '100%' }}>
        <Space>
          <Button
            icon={<ReloadOutlined />}
            onClick={handleRefreshAll}
            loading={refreshLoading}
            type="primary"
            danger
            size="large"
            disabled={refreshLoading}
          >
            모든 회차 정보 가져오기
          </Button>
          {refreshLoading && (
            <Button
              icon={<StopOutlined />}
              onClick={handleCancelRefresh}
              type="default"
              danger
              size="large"
            >
              중단
            </Button>
          )}
          <Divider type="vertical" />
          <Space>
            <Text>강제 업데이트:</Text>
            <Switch
              checked={forceUpdate}
              onChange={setForceUpdate}
              disabled={refreshLoading}
            />
            <Text type="secondary" style={{ fontSize: '12px' }}>
              {forceUpdate 
                ? '모든 회차를 다시 수집하여 업데이트 (당첨금 정보 포함)' 
                : '기존 데이터는 건너뛰고 누락된 회차만 수집'}
            </Text>
          </Space>
        </Space>
        <Text type="secondary" style={{ fontSize: '12px', marginTop: '-8px' }}>
          {!forceUpdate && '(DB 최신 회차부터 API 최신 회차까지 자동 수집)'}
        </Text>

        {refreshResult && (
          <Card size="small" title="수집 결과" style={{ marginTop: '16px' }}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <div>
                <Text strong>상태: </Text>
                <Tag color={refreshResult.success ? 'success' : 'error'}>
                  {refreshResult.success ? '성공' : '실패'}
                </Tag>
              </div>
              
              <div>
                <Text strong>메시지: </Text>
                <Text>{refreshResult.message}</Text>
              </div>

              {refreshResult.success && (
                <>
                  <div>
                    <Text strong>저장된 회차 수: </Text>
                    <Tag color="green">{refreshResult.savedCount}개</Tag>
                  </div>
                  <div>
                    <Text strong>실패한 회차 수: </Text>
                    <Tag color="red">{refreshResult.failedCount}개</Tag>
                  </div>
                  <div>
                    <Text strong>최신 회차: </Text>
                    <Tag color="blue">{refreshResult.latestDrawNo}회차</Tag>
                  </div>
                </>
              )}
            </Space>
          </Card>
        )}

        <Divider>또는</Divider>

        <Space>
          <Text strong>회차:</Text>
          <InputNumber
            min={1}
            value={drawNo || undefined}
            onChange={(value) => setDrawNo(value)}
            style={{ width: '150px' }}
            placeholder="회차 번호 입력"
            disabled={loading}
          />
          <Button
            icon={<CloudDownloadOutlined />}
            onClick={handleFetchAndSave}
            loading={loading}
            type="primary"
            disabled={!drawNo || drawNo < 1}
          >
            얻어오기
          </Button>
        </Space>

        {result && (
          <Card size="small" title="결과" style={{ marginTop: '16px' }}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <div>
                <Text strong>상태: </Text>
                <Tag color={result.success ? 'success' : 'error'}>
                  {result.success ? '성공' : '실패'}
                </Tag>
                {result.fetched && (
                  <Tag color="blue">조회 성공</Tag>
                )}
                {result.saved && (
                  <Tag color="green">{result.isUpdate ? '업데이트 완료' : '저장 완료'}</Tag>
                )}
              </div>
              
              <div>
                <Text strong>메시지: </Text>
                <Text>{result.message}</Text>
              </div>

              {result.data && (
                <div style={{ marginTop: '12px', padding: '12px', background: '#f5f5f5', borderRadius: '4px' }}>
                  <Paragraph>
                    <Text strong>회차:</Text> {result.data.drawNo}
                  </Paragraph>
                  <Paragraph>
                    <Text strong>추첨일:</Text> {result.data.drawDate}
                  </Paragraph>
                  <Paragraph>
                    <Text strong>번호:</Text>{' '}
                    <Space>
                      {result.data.numbers.map((num, idx) => (
                        <Tag key={idx} color="blue">{num}</Tag>
                      ))}
                      <Tag color="red">+{result.data.bonus}</Tag>
                    </Space>
                  </Paragraph>
                </div>
              )}

              {!result.fetched && (
                <Alert
                  message="조회 실패"
                  description="동행복권 API에서 데이터를 가져올 수 없습니다. 회차가 존재하지 않거나 API 오류가 발생했을 수 있습니다."
                  type="warning"
                  style={{ marginTop: '12px' }}
                />
              )}

              {result.fetched && !result.saved && (
                <Alert
                  message="저장 실패"
                  description="데이터 조회는 성공했지만 DB 저장에 실패했습니다."
                  type="error"
                  style={{ marginTop: '12px' }}
                />
              )}
            </Space>
          </Card>
        )}
      </Space>
    </div>
  );
};
