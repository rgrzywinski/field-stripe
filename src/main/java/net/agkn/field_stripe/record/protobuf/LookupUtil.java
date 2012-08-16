package net.agkn.field_stripe.record.protobuf;

/*
 * Copyright 2012 Aggregate Knowledge, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.agkn.field_stripe.exception.DeveloperException;
import net.agkn.field_stripe.exception.NoSuchObjectException;

import com.dyuproject.protostuff.parser.HasName;
import com.dyuproject.protostuff.parser.Message;
import com.dyuproject.protostuff.parser.Proto;

/**
 * A convenience utility for looking up {@link Message message definitions} 
 * within a {@link Proto Protobuf definition}.
 *
 * @author rgrzywinski
 */
public class LookupUtil {
    /**
     * Looks up the specified (possibly qualified) message name in the context
     * of the specified {@link Proto Protobuf definition}.
     * 
     * @param  protobufDefinition the <code>Proto</code> in which the specified
     *         message name is looked up. This cannot be <code>null</code> or
     *         blank.
     * @param  messageName the (possibly qualified) name of the desired message.
     *         This cannot be <code>null</code> or blank.
     * @return the desired message. This will never be <code>null</code>.
     * @throws NoSuchObjectException if the specified named message did not
     *         exist in this Protobuf definition.
     */
    public static Message lookupMessage(final Proto proto, final String messageName) 
            throws NoSuchObjectException {
        // NOTE:  this is all to get around the fact that the lookup methods on
        //        Proto are all 'package'.
        // TODO:  cache the method reference
        try {
            final Method findMessageReference = Proto.class.getDeclaredMethod("findMessageReference", new Class[] { String.class, String.class});
                findMessageReference.setAccessible(true);
            final HasName reference = (HasName)findMessageReference.invoke(proto, messageName, proto.getPackageName()/*assume the namespace is the package*/);
            if(reference instanceof Message)
                return (Message)reference;
            /* else -- the reference is either null or not a Message */
            throw new NoSuchObjectException("Message \"" + messageName + "\" could not be found.");
        } catch(final IllegalAccessException iae) {
            throw new DeveloperException("Unexpected illegal access exception from Proto#findMessageReference().", iae);
        } catch(final InvocationTargetException ite) {
            throw new DeveloperException("Unexpected exception from Proto#findMessageReference().", ite);
        } catch(final NoSuchMethodException nsme) {
            throw new DeveloperException(nsme);
        }
    }
}