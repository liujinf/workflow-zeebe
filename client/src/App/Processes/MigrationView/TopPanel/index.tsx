/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {Diagrams} from './Diagrams';
import {Header} from './Header';

const TopPanel: React.FC = () => {
  return (
    <section>
      <Header />
      <Diagrams />
    </section>
  );
};

export {TopPanel};
