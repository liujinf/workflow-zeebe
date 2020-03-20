/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import React from 'react';
import {shallow} from 'enzyme';

import ListItem from './ListItem';

it('should match snapshot', () => {
  const node = shallow(
    <ListItem
      data={{
        type: 'Item Type',
        name: 'Test List Entry',
        icon: 'process',
        link: '/report/1',
        meta: ['Column 1', 'Column 2'],
        actions: [{action: jest.fn(), icon: 'delete', text: 'Delete Entry'}],
        warning: 'Warning Text'
      }}
      hasWarning
    />
  );

  expect(node).toMatchSnapshot();
});

it('should not render a Link component if no link is provided', () => {
  const node = shallow(<ListItem data={{}} />);

  expect(node.find('Link')).not.toExist();
});

it('should have a warning column even if no specific warning exists for this ListItem', () => {
  const node = shallow(<ListItem data={{}} hasWarning />);

  expect(node.find('.warning')).toExist();
});
