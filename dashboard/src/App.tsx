import React, { useState, useMemo, useEffect } from 'react';
import { DecisionGraph, JdmConfigProvider } from '@gorules/jdm-editor';
import '@gorules/jdm-editor/dist/style.css';
import { Layout, Menu, Typography, Card, Space, Tag, Button, Divider, message, Spin } from 'antd';
import { ShieldCheck, Activity, Plus, Save, PlayCircle, FolderOpen } from 'lucide-react';
import './App.css';

const { Header, Content, Sider } = Layout;
const { Title, Text } = Typography;

function App() {
  const [rulesData, setRulesData] = useState<any[]>([]);
  const [selectedRule, setSelectedRule] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch('http://localhost:8080/api/rules')
      .then(res => res.json())
      .then(data => {
        setRulesData(data);
        if (data.length > 0) setSelectedRule(data[0]);
        setLoading(false);
      })
      .catch(e => {
        message.error('Failed to connect to Vert.x API Server');
        setLoading(false);
      });
  }, []);

  // Group rules by category for a cleaner, nested sidebar
  const menuItems = useMemo(() => {
    const grouped = rulesData.reduce((acc: any, rule: any) => {
      const cat = rule.category || 'Other';
      if (!acc[cat]) acc[cat] = [];
      acc[cat].push(rule);
      return acc;
    }, {});

    return Object.keys(grouped).map(cat => ({
      key: `cat-${cat}`,
      icon: <FolderOpen size={16} />,
      label: cat,
      children: grouped[cat].map((r: any) => ({
        key: r.id.toString(),
        icon: <Activity size={16} />,
        label: r.name.replace(/_/g, ' ').replace(/\b\w/g, (l: string) => l.toUpperCase()),
      }))
    }));
  }, [rulesData]);

  const onMenuClick = (id: string) => {
    if (id.startsWith('cat-')) return;
    const rule = rulesData.find((r: any) => r.id.toString() === id);
    if (rule) setSelectedRule(rule);
  };

  const handleRunAudit = () => {
    message.loading({ content: 'Executing Compliance Engine via Vert.x...', key: 'audit' });
    fetch('http://localhost:8080/api/audit/run', { method: 'POST' })
      .then(res => {
        if (res.ok) message.success({ content: 'Audit sequence successfully running.', key: 'audit', duration: 3 });
        else message.error({ content: 'Audit failed to run.', key: 'audit' });
      }).catch(e => message.error({ content: 'Network error triggering audit.', key: 'audit' }));
  };

  const handleSave = () => {
    if (!selectedRule) return;
    message.loading({ content: 'Saving to Postgres Database...', key: 'save' });
    fetch('http://localhost:8080/api/rules', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify([selectedRule])
    }).then(res => {
      if (res.ok) message.success({ content: 'Rule permanently saved via Vert.x!', key: 'save', duration: 2 });
      else message.error({ content: 'Database save failed.', key: 'save' });
    }).catch(e => message.error({ content: 'Network error saving rule.', key: 'save' }));
  };

  const handleCreateRule = () => {
    message.info('Opening Rule Architect form...');
  };

  const onGraphChange = (val: any) => {
    setSelectedRule((prev: any) => ({ ...prev, content: val }));
  };

  if (loading) {
    return <div style={{ height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#0f172a' }}>
      <Spin size="large" />
    </div>;
  }

  if (!selectedRule) return <div style={{ color: 'white' }}>No Enterprise Rules found in Database.</div>;

  return (
    <JdmConfigProvider>
      <Layout className="dashboard-layout">
        <Sider width={280} className="dashboard-sider">
          <div className="sider-header">
            <ShieldCheck size={28} className="logo-icon" />
            <span className="logo-text">HRM Audit Hub</span>
          </div>
          
          <div className="sider-actions">
            <Button type="primary" block icon={<Plus size={16} />} className="add-rule-btn" onClick={handleCreateRule}>
              Create New Rule
            </Button>
          </div>

          <Menu
            mode="inline"
            theme="dark"
            defaultOpenKeys={menuItems.map(m => m.key)}
            selectedKeys={[selectedRule.id.toString()]}
            onClick={({ key }) => onMenuClick(key)}
            items={menuItems}
            className="custom-menu"
          />
        </Sider>

        <Layout className="main-area">
          <Header className="dashboard-header">
            <div className="header-breadcrumbs">
              <Text type="secondary">Audit Policies</Text>
              <span className="separator">/</span>
              <Text strong>{selectedRule.category}</Text>
            </div>
            
            <Space size="middle">
              <Button icon={<PlayCircle size={16} />} onClick={handleRunAudit}>Run Audit</Button>
              <Button type="primary" icon={<Save size={16} />} onClick={handleSave}>Save Changes</Button>
            </Space>
          </Header>

          <Content className="dashboard-content">
            <Card className="rule-card" bordered={false}>
              <div className="card-top-bar">
                <div>
                  <Space direction="vertical" size={2}>
                    <Space size="middle" align="center">
                      <Title level={3} style={{ margin: 0, color: '#0f172a' }}>
                        {selectedRule.name.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())}
                      </Title>
                      <Tag color="cyan" style={{ margin: 0 }}>Active</Tag>
                    </Space>
                    <Text type="secondary" style={{ fontSize: '14px' }}>
                      {selectedRule.description}
                    </Text>
                  </Space>
                </div>
              </div>
              
              <div className="editor-wrapper">
                <DecisionGraph
                  value={selectedRule.content as any}
                  onChange={onGraphChange} 
                />
              </div>
            </Card>
          </Content>
        </Layout>
      </Layout>
    </JdmConfigProvider>
  );
}

export default App;
