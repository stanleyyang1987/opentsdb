// This file is part of OpenTSDB.
// Copyright (C) 2017  The OpenTSDB Authors.
//
// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 2.1 of the License, or (at your
// option) any later version.  This program is distributed in the hope that it
// will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
// General Public License for more details.  You should have received a copy
// of the GNU Lesser General Public License along with this program.  If not,
// see <http://www.gnu.org/licenses/>.
package net.opentsdb.data.iterators;

/**
 * An enumerator that describes the state of the iterator.
 * <h1>State Transitions</h1>
 * <p>
 * A new iterator can start in either {@link #HAS_DATA}, {@link #END_OF_CHUNK} or
 * {@link #EXCEPTION}.
 * <pre>
 * {@code
 * 
 *        +--------+
 * +------+HAS DATA<-----+
 * |      +--------+     |
 * |          |          |
 * |          +----------^
 * |          |          |
 * |    +-----v------+   |
 * v----+END OF CHUNK+---^
 * |    +------------+   
 * |          |          
 * |          |
 * |    +-----v-------+
 * +---->END OF STREAM|
 * |    +-------------+
 * |
 * |    +-------------+
 * +---->  EXCEPTION  |
 *      +-------------+
 * 
 * Courtesy: http://asciiflow.com/
 * }
 * </pre>
 * <p>
 * From {@link #HAS_DATA} the state can transition to {@link #HAS_DATA}, 
 * {@link #END_OF_CHUNK}, {@link #END_OF_DATA} or {@link #EXCEPTION}.
 * <p>
 * From {@link #END_OF_CHUNK} the state must transition {@link #HAS_DATA}, 
 * {@link #END_OF_DATA} or {@link #EXCEPTION}.
 * <p> 
 * {@link #END_OF_DATA} is a termination state. No more data can be read from
 * the stream and it can be closed.
 * <p>
 * {@link #EXCEPTION} is a termination state. An unrecoverable problem occurred
 * and the stream must be closed. Another stream can be created to retry the
 * operation.
 * 
 * @since 3.0
 */
public enum IteratorStatus {
  /** The stream has more data in memory ready for processing. */
  HAS_DATA,
  
  /** The stream does not have any more data in memory but there may be more
   * at the source. Call {@code fetch()} on the iterator to check for and load
   * additional data. */
  END_OF_CHUNK,
  
  /** A terminal condition that indicates no more data is available for this
   * stream and it can be closed. */
  END_OF_DATA,
  
  /** A terminal condition that indicates that an unrecoverable exception occurred
   * and the stream must be closed. A new stream can be instantiated for a retry. */
  EXCEPTION;
  
  /**
   * Decision tree used by a processor to determine how to update the local
   * status given a child iterator (or processor) status.
   * @param existing The existing status of the processor.
   * @param new_status The new status from the child iterator.
   * @return What the new processor's status should be.
   * @throws IllegalStateException if the status transition is unsupported.
   */
  public static IteratorStatus updateStatus(final IteratorStatus existing, 
      final IteratorStatus new_status) {
    if (existing == IteratorStatus.EXCEPTION || new_status == IteratorStatus.EXCEPTION) {
      return IteratorStatus.EXCEPTION;
    }
    // ugly decision tree
    switch (new_status) {
    case HAS_DATA:
      return IteratorStatus.HAS_DATA;
      
    case END_OF_CHUNK:
      switch (existing) {
      case HAS_DATA:
        return IteratorStatus.HAS_DATA;
      default:
        return IteratorStatus.END_OF_CHUNK; // let all chunks read EOC since they're time synced
      }
      
    case END_OF_DATA:
      switch (existing) {
      case HAS_DATA:
        return IteratorStatus.HAS_DATA;
      case END_OF_CHUNK:
        return IteratorStatus.END_OF_CHUNK;
      default:
        return IteratorStatus.END_OF_DATA;
      }
    default:
      throw new IllegalStateException("Cannot move from existing status [" 
          + existing + "] to status [" + new_status +"]");
    }
  }
}