/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global Ext, NX*/

/**
 * Nodes feature panel.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.system.Nodes', {
  extend: 'NX.view.drilldown.Drilldown',
  alias: 'widget.nx-coreui-system-nodes',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function() {

    var me = this;
    me.iconName = 'node-default';
    if (NX.app.Application.bundleActive('com.sonatype.nexus.plugins.nexus-hazelcast-plugin')
        && NX.State.getValue('nodes', {})['enabled']) {
      me.masters = [{xtype: 'nx-coreui-system-nodelist'}];
      me.tabs = {
        xtype: 'nx-info-panel',
        title: NX.I18n.get('System_Bundles_Details_Tab')
      };
    }
    else {
      me.masters = [{xtype: 'nx-coreui-system-nodes-disabled'}];
      me.tabs = undefined;
      me.skipDetail = true;
    }

    this.callParent();
  }
});
