/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import React from 'react';
import {Link} from 'react-router-dom';

import './ErrorPage.scss';
import {t} from 'translation';

export default function ErrorPage({children, noLink, text}) {
  return (
    <div className="ErrorPage">
      <h1>{text || t('common.errors.inValidLink')}</h1>
      {!noLink && <Link to="/">{t('common.goToHome')}…</Link>}
      {children}
    </div>
  );
}
