/*
 * Copyright 2014 Cazcade Limited (http://cazcade.com)
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

package exec;


/**
 * @author Neil Ellis
 */

public interface HashedExecutorService extends ServiceStateMachine {
    /**
     * Execute some executable code in one of a pool of threads. If retry is specified (that is environmental conditions
     * could change and allow a succesful completion at a later date) then the job will be executed multiple times.
     * <br/> The hashObject is used to reduce collisitons, so for example if the hash object is a file name and the job
     * is writing to that filename then no two commands can alter that file simultaneously. This enforces single threaded
     * behaviour towards a given resource. <br/> Note that if the execution queue is full for the job this method will
     * block until succesful queueing.
     *
     * @param retry      should execution be re-attempted on failure.
     * @param hashObject an object which determines through it's hashcode which thread this should be executed on.
     * @param executable some executable code.
     * @throws InterruptedException if the submission thread (this thread) is interrupted while attempting to queue.
     */
    void execute(boolean retry, Object hashObject, HashedExecutable executable) throws InterruptedException;

    void submit(boolean retry, boolean skipIfFull, Object key, HashedExecutable executable) throws InterruptedException;

    /**
     * No retry, random hash.
     *
     * @see #execute(boolean, Object, HashedExecutable)
     */
    void execute(HashedExecutable executable) throws InterruptedException;

    /**
     * Waits until all the thread pools are empty, do not submit new work while this is running.
     *
     * @throws InterruptedException if the thread is interrupted.
     */
    void waitForExecutionsToFinish() throws InterruptedException;
}
