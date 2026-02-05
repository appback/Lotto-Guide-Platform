import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { MainPage } from '@/pages/MainPage';
import { AdminPage } from '@/pages/AdminPage';

const AppRouter: React.FC = () => (
  <BrowserRouter basename="/lotto">
    <Routes>
      <Route path="/" element={<MainPage />} />
      <Route path="/admin" element={<AdminPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  </BrowserRouter>
);

export default AppRouter;
