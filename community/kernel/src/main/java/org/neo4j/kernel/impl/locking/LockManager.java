/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.locking;

import org.neo4j.kernel.DeadlockDetectedException;
import org.neo4j.kernel.impl.locking.community.LockNotFoundException;
import org.neo4j.kernel.impl.transaction.IllegalResourceException;
import org.neo4j.logging.Logger;

/**
 * The LockManager can lock resources for reading or writing. By doing this one
 * may achieve different transaction isolation levels. A resource can for now be
 * any object (but null).
 * <p>
 * Acquiring locks must be released after usage. Failure to do so will result in
 * the resource being blocked to all other transactions. Put all locks in a try -
 * finally block.
 * <p>
 * Multiple locks on the same resource held by the same transaction requires the
 * transaction to invoke the release lock method multiple times. If a tx has
 * invoked {@link #getReadLock(Object, Object)} on the same resource x times in
 * a row it must invoke {@link #releaseReadLock(Object, Object)} x times to release
 * all the locks. Same for write locks correspondingly.
 * <p>
 * LockManager just maps locks to resources and they do all the hard work
 * together with a {@link org.neo4j.kernel.impl.locking.community.RagManager resource allocation graph}.
 */
public interface LockManager
{
    /**
     * Tries to acquire read lock on {@code resource} for a given
     * transaction. If read lock can't be acquired the transaction will wait for
     * the transaction until it can acquire it. If waiting leads to dead lock a
     * {@link DeadlockDetectedException} will be thrown.
     * Waiting can also be terminated. In that case waiting thread will be interrupted and corresponding
     * {@link org.neo4j.kernel.impl.locking.community.RWLock.TxLockElement} will be marked as terminated.
     * In that case lock will not be acquired and false will be return as result of acquisition
     *
     * @param resource the resource to lock
     * @throws DeadlockDetectedException if a deadlock is detected, or prevented rather
     * @throws IllegalResourceException if an illegal resource is supplied
     * @return true is lock was acquired, false otherwise
     */
    boolean getReadLock( Object resource, Object tx )
            throws DeadlockDetectedException, IllegalResourceException;

    /**
     * Tries to acquire read lock on {@code resource} for a given
     * transaction. If read lock can't be acquired {@code false} will be returned
     * and no lock will have been acquired.
     *
     * @param resource the resource
     * @throws IllegalResourceException if an illegal resource is supplied
     * @return true is lock was acquired, false otherwise
     */
    boolean tryReadLock( Object resource, Object tx )
            throws IllegalResourceException;

    /**
     * Tries to acquire write lock on <CODE>resource</CODE> for a given
     * transaction. If write lock can't be acquired the transaction will wait
     * for the lock until it can acquire it. If waiting leads to dead lock a
     * {@link DeadlockDetectedException} will be thrown.
     * Waiting can also be terminated. In that case waiting thread will be interrupted and corresponding
     * {@link org.neo4j.kernel.impl.locking.community.RWLock.TxLockElement} will be marked as terminated.
     * In that case lock will not be acquired and false will be return as result of acquisition
     *
     * @param resource the resource
     * @throws DeadlockDetectedException if a deadlock is detected, or prevented rather
     * @throws IllegalResourceException if an illegal resource is supplied
     * @return true is lock was acquired, false otherwise
     */
    boolean getWriteLock( Object resource, Object tx )
            throws DeadlockDetectedException, IllegalResourceException;

    /**
     * Tries to acquire write lock on {@code resource} for a given
     * transaction. If write lock can't be acquired {@code false} will be returned
     * and no lock will have been acquired.
     *
     * @param resource the resource
     * @throws IllegalResourceException if an illegal resource is supplied
     * @return true is lock was acquired, false otherwise
     */
    boolean tryWriteLock( Object resource, Object tx )
            throws IllegalResourceException;

    /**
     * Releases a read lock held by the given transaction on {@code resource}.
     *
     * @param resource the resource
     * @throws IllegalResourceException if an illegal resource is supplied
     * @throws org.neo4j.kernel.impl.locking.community.LockNotFoundException if given transaction don't have read lock on the given {@code resource}.
     */
    void releaseReadLock( Object resource, Object tx )
            throws LockNotFoundException, IllegalResourceException;

    /**
     * Releases a write lock held by the given transaction on {@code resource}.
     *
     * @param resource the resource
     * @throws IllegalResourceException if an illegal resource is supplied
     * @throws LockNotFoundException if given transaction don't have read lock on the given {@code resource}.
     */
    void releaseWriteLock( Object resource, Object tx )
            throws LockNotFoundException, IllegalResourceException;

    /**
     * @return number of deadlocks that have been detected and prevented.
     * @see #getWriteLock(Object, Object) and {@link #getReadLock(Object, Object)}.
     */
    long getDetectedDeadlockCount();

    /**
     * Utility method for debugging. Dumps info to {@code logger} about txs having locks on resources.
     */
    void dumpLocksOnResource( final Object resource, Logger logger );

}