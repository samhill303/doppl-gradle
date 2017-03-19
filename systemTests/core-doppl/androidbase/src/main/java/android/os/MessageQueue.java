/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os;

import android.util.AndroidRuntimeException;
import android.util.Log;

import com.google.j2objc.annotations.Weak;

/**
 * Low-level class holding the list of messages to be dispatched by a
 * {@link Looper}.  Messages are not added directly to a MessageQueue,
 * but rather through {@link Handler} objects associated with the Looper.
 *
 * <p>You can retrieve the MessageQueue for the current thread with
 * {@link Looper#myQueue() Looper.myQueue()}.
 */
public class MessageQueue {
    @Weak
    private final ThreadNudger mThreadNudger;
    Message mMessages;
    //    private final ArrayList<IdleHandler> mIdleHandlers = new ArrayList<IdleHandler>();
    //    private IdleHandler[] mPendingIdleHandlers;
    private boolean mQuiting;
    boolean mQuitAllowed = true;

    // Indicates whether next() is blocked waiting in pollOnce() with a non-zero timeout.
    private boolean mBlocked = true;

    @SuppressWarnings("unused")
    private int mPtr; // used by native code

    /*-[
        NSOperationQueue *opQueue;
     ]-*/
    
    private native void nativeInit()/*-[
        if([NSThread isMainThread])
            opQueue = [NSOperationQueue mainQueue];
        else
            opQueue = [[NSOperationQueue alloc] init];
     ]-*/;

    private native void nativeDestroy()/*-[

     ]-*/;

    private void dopNudgeThreadNow()
    {
        dopNudgeThreadAt(0);
    }

    private native void dopNudgeThreadAt(int timeoutMillis)/*-[
    if(timeoutMillis <= 0){
        [self callNudgeOnThread];
    }else{
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (long)timeoutMillis + (long)1000000), dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            [self callNudgeOnThread];
        });
    }
    ]-*/;

    private void testThreadTimer(final int timeout)
    {
        new Thread(){
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep(timeout);
                    dopNudgeThreadNow();
                }
                catch(InterruptedException e)
                {
                    //
                }
            }
        }.start();
    }

    private native void callNudgeOnThread()/*-[
        [opQueue addOperation:[NSBlockOperation blockOperationWithBlock:^{
            [self callNudge];
        }]];
    ]-*/;

    private void callNudge()
    {
        mThreadNudger.nudge();
    }

    /**
     * Callback interface for discovering when a thread is going to block
     * waiting for more messages.
     */
    public static interface IdleHandler
    {
        /**
         * Called when the message queue has run out of messages and will now
         * wait for more.  Return true to keep your idle handler active, false
         * to have it removed.  This may be called if there are still messages
         * pending in the queue, but they are all scheduled to be dispatched
         * after the current time.
         */
        boolean queueIdle();
    }

    /**
     * Add a new {@link IdleHandler} to this message queue.  This may be
     * removed automatically for you by returning false from
     * {@link IdleHandler#queueIdle IdleHandler.queueIdle()} when it is
     * invoked, or explicitly removing it with {@link #removeIdleHandler}.
     *
     * <p>This method is safe to call from any thread.
     *
     * @param handler The IdleHandler to be added.
     */
    public final void addIdleHandler(IdleHandler handler) {
        throw new UnsupportedOperationException("We will be handing control back to native iOS thread, so no idle. " +
                                                        "May add back in the future.");
    }

    /**
     * Remove an {@link IdleHandler} from the queue that was previously added
     * with {@link #addIdleHandler}.  If the given object is not currently
     * in the idle list, nothing is done.
     *
     * @param handler The IdleHandler to be removed.
     */
    public final void removeIdleHandler(IdleHandler handler) {
        throw new UnsupportedOperationException("We will be handing control back to native iOS thread, so no idle. " +
                                                        "May add back in the future.");
    }

    MessageQueue(ThreadNudger threadNudger) {
        mThreadNudger = threadNudger;
        nativeInit();
    }

    /**
     * Check and return next message. The standard Android version will block until a message is ready.
     * We're modifying this slighly, as thread management is delegated to the host iOS environment.
     * As such, we'll schedule a nudge timer if necessary, but return null if no message is ready.
     * The looper can exit and return control to the host thread.
     *
     * We'll need to be careful not to queue up multiple calls to nudge if they're not necessary.  Because
     * a single caller thread is running (presumably ;), it should be a logic problem, but in extreme cases
     * it could be a performance issue. At least it'll cause unnecessary effort.
     * @return
     */
    final Message next() {
        synchronized (this) {
            // Try to retrieve the next message.  Return if found.
            final long now = SystemClock.uptimeMillis();
            final Message msg = mMessages;
            if (msg != null) {
                final long when = msg.when;
                if (now >= when) {
                    mBlocked = false;
                    mMessages = msg.next;
                    msg.next = null;
                    if (false) Log.v("MessageQueue", "Returning message: " + msg);
                    msg.markInUse();
                    return msg;
                } else {
                    final int nudgeTimeout = (int) Math.min(when - now, Integer.MAX_VALUE);
                    dopNudgeThreadAt(nudgeTimeout);
                }
            }

            mBlocked = true;
            return null;
        }
    }

    final boolean enqueueMessage(Message msg, long when) {
        if (msg.isInUse()) {
            throw new AndroidRuntimeException(msg
                                                      + " This message is already in use.");
        }
        if (msg.target == null && !mQuitAllowed) {
            throw new RuntimeException("Main thread not allowed to quit");
        }
        final boolean needWake;
        synchronized (this) {
            if (mQuiting) {
                RuntimeException e = new RuntimeException(
                        msg.target + " sending message to a Handler on a dead thread");
                Log.w("MessageQueue", e.getMessage(), e);
                return false;
            } else if (msg.target == null) {
                mQuiting = true;
            }

            msg.when = when;
            //Log.d("MessageQueue", "Enqueing: " + msg);
            Message p = mMessages;
            if (p == null || when == 0 || when < p.when) {
                msg.next = p;
                mMessages = msg;
                needWake = mBlocked; // new head, might need to wake up
            } else {
                Message prev = null;
                while (p != null && p.when <= when) {
                    prev = p;
                    p = p.next;
                }
                msg.next = prev.next;
                prev.next = msg;
                needWake = false; // still waiting on head, no need to wake up
            }
        }
        if (needWake) {
//            nativeWake(mPtr);
            dopNudgeThreadNow();
        }
        return true;
    }

    final boolean removeMessages(Handler h, int what, Object object,
            boolean doRemove) {
        synchronized (this) {
            Message p = mMessages;
            boolean found = false;

            // Remove all messages at front.
            while (p != null && p.target == h && p.what == what
                    && (object == null || p.obj == object)) {
                if (!doRemove) return true;
                found = true;
                Message n = p.next;
                mMessages = n;
                p.recycle();
                p = n;
            }

            // Remove all messages after front.
            while (p != null) {
                Message n = p.next;
                if (n != null) {
                    if (n.target == h && n.what == what
                            && (object == null || n.obj == object)) {
                        if (!doRemove) return true;
                        found = true;
                        Message nn = n.next;
                        n.recycle();
                        p.next = nn;
                        continue;
                    }
                }
                p = n;
            }

            return found;
        }
    }

    final void removeMessages(Handler h, Runnable r, Object object) {
        if (r == null) {
            return;
        }

        synchronized (this) {
            Message p = mMessages;

            // Remove all messages at front.
            while (p != null && p.target == h && p.callback == r
                    && (object == null || p.obj == object)) {
                Message n = p.next;
                mMessages = n;
                p.recycle();
                p = n;
            }

            // Remove all messages after front.
            while (p != null) {
                Message n = p.next;
                if (n != null) {
                    if (n.target == h && n.callback == r
                            && (object == null || n.obj == object)) {
                        Message nn = n.next;
                        n.recycle();
                        p.next = nn;
                        continue;
                    }
                }
                p = n;
            }
        }
    }

    final void removeCallbacksAndMessages(Handler h, Object object) {
        synchronized (this) {
            Message p = mMessages;

            // Remove all messages at front.
            while (p != null && p.target == h
                    && (object == null || p.obj == object)) {
                Message n = p.next;
                mMessages = n;
                p.recycle();
                p = n;
            }

            // Remove all messages after front.
            while (p != null) {
                Message n = p.next;
                if (n != null) {
                    if (n.target == h && (object == null || n.obj == object)) {
                        Message nn = n.next;
                        n.recycle();
                        p.next = nn;
                        continue;
                    }
                }
                p = n;
            }
        }
    }

    /*
    private void dumpQueue_l()
    {
        Message p = mMessages;
        System.out.println(this + "  queue is:");
        while (p != null) {
            System.out.println("            " + p);
            p = p.next;
        }
    }
    */
}
