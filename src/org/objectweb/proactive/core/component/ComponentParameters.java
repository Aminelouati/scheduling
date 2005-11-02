/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2005 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@objectweb.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://www.inria.fr/oasis/ProActive/contacts.html
 *  Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.core.component;

import java.io.Serializable;
import java.util.ArrayList;

import org.objectweb.fractal.api.type.ComponentType;
import org.objectweb.fractal.api.type.InterfaceType;
import org.objectweb.proactive.core.component.type.ProActiveComponentType;


/** Contains the configuration of a component. <ul>
 * <li> type</li>
 * <li> interfaces (server and client) --> in contained ControllerDescription object</li>
 * <li> name --> in contained ControllerDescription object</li>
 * <li> hierarchical type (primitive or composite) --> in contained ControllerDescription object</li>
 * <li> a ref on the stub on the base object</li>
 * </ul>
 */
public class ComponentParameters implements Serializable {
    private Object stubOnReifiedObject;
    private ComponentType componentType;
    private ControllerDescription controllerDesc;

    /** Constructor for ComponentParameters.
     * @param name the name of the component
     * @param hierarchicalType the hierarchical type, either PRIMITIVE or COMPOSITE or PARALLEL
     * @param componentType
     */
    public ComponentParameters(String name, String hierarchicalType,
        ComponentType componentType) {
        this(componentType, new ControllerDescription(name, hierarchicalType));
    }

    public ComponentParameters(String name, String hierarchicalType,
        ComponentType componentType, String controllerConfigFileLocation) {
        this(componentType, new ControllerDescription(name, hierarchicalType));
    }

    /**
     * Constructor
     * @param componentType the type of the component
     * @param controllerDesc a ControllerDescription object
     */
    public ComponentParameters(ComponentType componentType,
        ControllerDescription controllerDesc) {
        this.componentType = componentType;
        this.controllerDesc = controllerDesc;
    }

    /**
     * overrides the clone method of Object
     * @return a clone of this current object
     */
    public Object clone() {
        return new ComponentParameters(new ProActiveComponentType(componentType),
            new ControllerDescription(controllerDesc));
    }

    /**
     * setter for the name
     * @param name name of the component
     */
    public void setName(String name) {
        controllerDesc.setName(name);
    }

    /**
     * Returns the componentType.
     * @return ComponentType
     */
    public ComponentType getComponentType() {
        return componentType;
    }

    /**
     * getter
     * @return a ControllerDescription object
     */
    public ControllerDescription getControllerDescription() {
        return controllerDesc;
    }

    /**
     * setter
     * @param componentType the type of the component
     */
    public void setComponentType(ComponentType componentType) {
        this.componentType = componentType;
    }

    /**
     * setter
     * @param string the hierarchical type (primitive, composite or parallel)
     */
    public void setHierarchicalType(String string) {
        controllerDesc.setHierarchicalType(string);
    }

    /**
     * getter
     * @return the name
     */
    public String getName() {
        return controllerDesc.getName();
    }

    /**
     * Returns the hierarchicalType.
     * @return String
     */
    public String getHierarchicalType() {
        return controllerDesc.getHierarchicalType();
    }

    /**
     * @return the types of server interfaces
     */
    public InterfaceType[] getServerInterfaceTypes() {
        ArrayList server_interfaces = new ArrayList();
        InterfaceType[] interfaceTypes = componentType.getFcInterfaceTypes();
        for (int i = 0; i < interfaceTypes.length; i++) {
            if (!interfaceTypes[i].isFcClientItf()) {
                server_interfaces.add(interfaceTypes[i]);
            }
        }
        return (InterfaceType[]) server_interfaces.toArray(new InterfaceType[server_interfaces.size()]);
    }

    /**
     * @return the types of client interfacess
     */
    public InterfaceType[] getClientInterfaceTypes() {
        return Fractive.getClientInterfaceTypes(componentType);
    }

    /**
     * getter
     * @return a table of interface types
     */
    public InterfaceType[] getInterfaceTypes() {
        return componentType.getFcInterfaceTypes();
    }
}
