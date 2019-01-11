/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.tomcat.util.modeler.modules;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.management.ObjectName;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.modeler.ManagedBean;
import org.apache.tomcat.util.modeler.Registry;

public class MbeansDescriptorsDigesterSource extends ModelerSource
{
    private static final Log log =
            LogFactory.getLog(MbeansDescriptorsDigesterSource.class);
    private static final Object dLock = new Object();

    private Registry registry;
    private final List<ObjectName> mbeans = new ArrayList<>();
    private static Digester digester = null;

    /**
     * ����digester����
     * @return
     */
    private static Digester createDigester() {

        Digester digester = new Digester();
        digester.setNamespaceAware(false);
        digester.setValidating(false);
        URL url = Registry.getRegistry(null, null).getClass().getResource
            ("/org/apache/tomcat/util/modeler/mbeans-descriptors.dtd");
        digester.register
            ("-//Apache Software Foundation//DTD Model MBeans Configuration File",
                url.toString());

        // Configure the parsing rules
        //����ManagedBean����
        digester.addObjectCreate
            ("mbeans-descriptors/mbean",
            "org.apache.tomcat.util.modeler.ManagedBean");
        //���ú������ļ�������ͬ��������
        digester.addSetProperties
            ("mbeans-descriptors/mbean");
        //���õ�ջ��Ԫ����
        digester.addSetNext
            ("mbeans-descriptors/mbean",
                "add",
            "java.lang.Object");
        //��������AttributeInfo
        digester.addObjectCreate
            ("mbeans-descriptors/mbean/attribute",
            "org.apache.tomcat.util.modeler.AttributeInfo");
        digester.addSetProperties
            ("mbeans-descriptors/mbean/attribute");
        //���ö������õ�ManagedBean
        digester.addSetNext
            ("mbeans-descriptors/mbean/attribute",
                "addAttribute",
            "org.apache.tomcat.util.modeler.AttributeInfo");
        //����NotificationInfo����
        digester.addObjectCreate
            ("mbeans-descriptors/mbean/notification",
            "org.apache.tomcat.util.modeler.NotificationInfo");
        digester.addSetProperties
            ("mbeans-descriptors/mbean/notification");
        //���ö������õ�ManagedBean
        digester.addSetNext
            ("mbeans-descriptors/mbean/notification",
                "addNotification",
            "org.apache.tomcat.util.modeler.NotificationInfo");
        
        //����FieldInfo����
        digester.addObjectCreate
            ("mbeans-descriptors/mbean/notification/descriptor/field",
            "org.apache.tomcat.util.modeler.FieldInfo");
        digester.addSetProperties
            ("mbeans-descriptors/mbean/notification/descriptor/field");
        digester.addSetNext
            ("mbeans-descriptors/mbean/notification/descriptor/field",
                "addField",
            "org.apache.tomcat.util.modeler.FieldInfo");
        //����һ������,��������ù����������ģʽʱ����ջ����Ԫ�صĸ÷���addNotifType
        digester.addCallMethod
            ("mbeans-descriptors/mbean/notification/notification-type",
                "addNotifType", 0);
        
        //��������OperationInfo
        digester.addObjectCreate
            ("mbeans-descriptors/mbean/operation",
            "org.apache.tomcat.util.modeler.OperationInfo");
        digester.addSetProperties
            ("mbeans-descriptors/mbean/operation");
        digester.addSetNext
            ("mbeans-descriptors/mbean/operation",
                "addOperation",
            "org.apache.tomcat.util.modeler.OperationInfo");
        
        //��������FieldInfo
        digester.addObjectCreate
            ("mbeans-descriptors/mbean/operation/descriptor/field",
            "org.apache.tomcat.util.modeler.FieldInfo");
        digester.addSetProperties
            ("mbeans-descriptors/mbean/operation/descriptor/field");
        digester.addSetNext
            ("mbeans-descriptors/mbean/operation/descriptor/field",
                "addField",
            "org.apache.tomcat.util.modeler.FieldInfo");
        
        //��������ParameterInfo
        digester.addObjectCreate
            ("mbeans-descriptors/mbean/operation/parameter",
            "org.apache.tomcat.util.modeler.ParameterInfo");
        digester.addSetProperties
            ("mbeans-descriptors/mbean/operation/parameter");
        digester.addSetNext
            ("mbeans-descriptors/mbean/operation/parameter",
                "addParameter",
            "org.apache.tomcat.util.modeler.ParameterInfo");

        return digester;

    }

    public void setRegistry(Registry reg) {
        this.registry=reg;
    }


    public void setSource( Object source ) {
        this.source=source;
    }

    @Override
    public List<ObjectName> loadDescriptors( Registry registry, String type,
            Object source) throws Exception {
        setRegistry(registry);
        setSource(source);
        execute();
        return mbeans;
    }

    public void execute() throws Exception {
        if (registry == null) {
            registry = Registry.getRegistry(null, null);
        }

        InputStream stream = (InputStream) source;

        ArrayList<ManagedBean> loadedMbeans = new ArrayList<>();
        synchronized(dLock) {
            if (digester == null) {
                digester = createDigester();
            }

            // Process the input file to configure our registry
            try {
                // Push our registry object onto the stack
                digester.push(loadedMbeans);
                //ͨ���ⲿ����source,xml�ļ�������������
                digester.parse(stream);
            } catch (Exception e) {
                log.error("Error digesting Registry data", e);
                throw e;
            } finally {
                digester.reset();
            }

        }
        Iterator<ManagedBean> iter = loadedMbeans.iterator();
        while (iter.hasNext()) {
            registry.addManagedBean(iter.next());
        }
    }
}