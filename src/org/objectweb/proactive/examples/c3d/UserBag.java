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
package org.objectweb.proactive.examples.c3d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Storage for triples of (int -> User  -> String).
 * This is a Bag, because keys can be the same [even though they shouldn't be!].
 */
public class UserBag implements Serializable {
    private ArrayList list = new ArrayList();

    /** Create an empty Bag. */
    public UserBag() {
    }

    /**
     * Does not erase previous equal key, only makes another pair with this key.
     */
    public void add(int key, User value, String name) {
        list.add(new Troika(key, value, name));
    }

    /**
     * The number of triples in this bag.
     */
    public int size() {
        return list.size();
    }

    /**
     * The C3DUser which is attached to the FIRST occurence of this key in the Bag.
     * @returns null if no element corresponds
     */
    public User getUser(int key) {
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Troika pair = (Troika) iter.next();
            if (pair.key == key) {
                return pair.user;
            }
        }
        return null;
    }

    /**
     * Returns the name of the FIRST occurence of this key in the Bag.
     * @returns "" if no element corresponds, because I don't like null Strings.
     */
    public String getName(int key) {
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Troika pair = (Troika) iter.next();
            if (pair.key == key) {
                return pair.name;
            }
        }
        return "";
    }

    /**
     * Removes FIRST occurence of this key in the Bag, with its value
     * This does not allow to retrieve the name, which is lost.
     * To get the name, first use a getName().
     * @return the associated C3DUser, if there was one (null otherwise).
     */
    public User remove(int key) {
        int max = size();
        for (int i = 0; i < max; i++) {
            Troika pair = (Troika) list.get(i);
            if (pair.key == key) {
                list.remove(i);
                return pair.user;
            }
        }
        return null;
    }

    // iteration related variables
    private transient Iterator iterator;
    private transient Troika currentTroika;

    // Iteration primitives, just like any Java Collection
    public void newIterator() {
        iterator = list.iterator();
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public void next() {
        currentTroika = (Troika) iterator.next();
    }

    // On the current triple pointed; get the different values. 
    public int currentKey() {
        return currentTroika.key;
    }

    public User currentUser() {
        return currentTroika.user;
    }

    public String currentName() {
        return currentTroika.name;
    }

    // EXAMPLE USE :
    //    for (userBag.newIterator(); userBag.hasNext(); ) {
    //        userBag.next();
    //        ( userBag.currentUser()).showMessage(s_message);
    //    }
    // A container for (int -> C3DUser -> String)
    private class Troika implements Serializable {
        int key;
        User user;
        String name;

        public Troika() {
        }

        public Troika(int key, User value, String name) {
            this.key = key;
            this.user = value;
            this.name = name;
        }
    }
}
