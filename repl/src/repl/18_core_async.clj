(ns repl.18-core-async
  (:require [clojure.core.async :as async :refer [<! <!! >! >!! chan go]]))


(comment

  ;; You create a channel with (chan):

  ;; the exchanges between channels are always between one producer and
  ;; one consumer
  (def c (chan))
  c







  ;; put! Waits for a process
  ;; to which it can give the value
  (async/put! c 42)



  ;; optionally put! can take a callback:
  ;; the argument is true when someone took the value
  (async/put! c 420 (fn [res] (println :done-2 res)))


  ;; take! always takes a callback,
  ;; which processes the value taken from the channel
  (async/take! c (fn [v] (println :got-1 v)))
  (async/take! c (fn [v] (println :got-2 v)))





  ;; Is there always somebody to take the value we put on the channel?

  (async/put! c :hello? (fn [res] (println :done res)))
  (async/take! c (fn [v] (println :got v)))

  (async/close! c)
  (async/put! c :hello? (fn [res] (println :done res)))
  (async/take! c (fn [v] (println :got v)))


  ;; if the channel is closed, it will not accept any more values;
  ;; once everything has been processed, it only returns nil from that point on.
  ;; How do you distinguish the nil returned by a closed channel from the value nil



  (def c (chan)) ;; new channel, closed channels remain closed
  (async/put! c nil)


  ;; The order of put! and take! does not matter, as they wait for each other
  (async/take! c (fn [v] (println :got v)))
  (async/put! c 42)



  ;; I promised before
  ;; that core.async avoids callback-hell

  ;; but there are callbacks EVERYWHERE?
  ;; What now?

  ;; the callbacks usually don't matter
  ;; I just want to exchange values


  ;; How does that work?




  ;; promises to the rescue!

  (defn take-promise [c]
    (let [p (promise)]
      (async/take! c (fn [v] (deliver p v)))
      @p))

  (future (println :was-promised (take-promise c)))
  (async/put! c 42)


  ;; analogous:

  (defn put-promise [c v]
    (let [p (promise)]
      (async/put! c v (fn [v] (deliver p v)))
      @p))

  (future (println :put-promise (put-promise c 9001)))
  (future (println :was-promised (take-promise c)))



  ;; take-promise and put-promise are already built in
  ;; they are called '<!!' and '>!!'

  ;; one exclamation mark means that they are not transactional
  ;; e.g. the retry of a swap! would put the value on the channel
  ;; multiple times or attempt to read multiple values from a channel

  ;; the other exclamation mark means that the call is blocking


  (future (println :got (<!! c)))
  (future (println :put (>!! c 1337)))


  ;; You can write something like this more idiomatically for core.async Style like this:

  (async/thread (>!! c 1337))
  (def tc (async/thread (<!! c)))
  (<!! tc)
  (<!! tc)

  ;; thread is almost like future:
  ;; thread executes the body in a new thread
  ;; and returns a channel,
  ;; which returns the result of the calculation
  ;; before closing itself





  ;; You can write decent code with this.
  ;; Disadvantages?





  ;; Many simultaneous puts and takes are problematic.
  ;; Because you will have a real thread, for every on of these operation, in the background
  ;; This can get really expensive; computationally
  ;; as well as in terms of memory.



  ;; Clojurescript has no futures oder promises.
  ;; This is why core.async in Clojurescript
  ;; does not even have >!! or <!! as operators.



  ;; How can you structure this without consuming a CPU thread every time?





  ;; Callbacks!



  ;; wat





  ;; This is the magic of the go-macro!
  ;; inside of (go ...) you can use <! and >!,
  ;; with the semantics of <!! and >!! respectively.

  ;; BUT:

  ;; go builds a state machine from the body
  ;; and <! as well as >! are included in the callback,
  ;; which is how it drives the state machine.

  ;; If the other party is not there at that moment,
  ;; someone else will continue their own work.
  ;; If at some point the other party does interact with the channel,
  ;; the callback is executed and the state machine continues into the next state.


  ;; WOW!


  ;; We have already seen the go-macro expanded for a simple call:
  ;; The end result is A LOT of generated code.
  ;; go consists of several thousand lines of Clojure code.

  ;; There are about 2 hours of videos on YouTube
  ;; of the author explaining some concepts -
  ;; we just accept for the sake of simplicity, that "it just works".



  (go (println :got (<! c)))

  (go (>! c 777))


  ;; go does have its limitations:
  ;; it can not look inside functions:

  (go ((fn [] (println (<! c)))))

  ;; therefore things like this also do not work:
  (go (println (map <! [c])))

  ;; or
  (go (println (for [e [c]] (<! e))))


  ;; but this works
  (go (doseq [e [c c c]]
        (println (<! e))))
  (go (>! c 1000))


  ;; we also observe that (go ...) returns a channel
  ;; it contains the result of the body



  ;; Alternatives


  (def a (chan))
  (def b (chan))

  ;; alts! takes a list of channels that can be read from

  (go (println :alts (async/alts! [a b])))
  (go (>! b :b))

  ;; we note in the output:
  ;; the return value of alts! is a tuple of the form
  ;; [the read value, the channel that "won"]


  (dotimes [_ 3] (go (>! a :a)))
  (dotimes [_ 3] (go (>! b :b)))

  (go (println (first (async/alts! [a b]))))
  (go (println (first (async/alts! [a b]))))
  (go (println (first (async/alts! [a b]))))
  (go (println (first (async/alts! [a b]))))
  (go (println (first (async/alts! [a b]))))
  (go (println (first (async/alts! [a b]))))

  ;; One of the alternatives is actually chosen at random,
  ;; if inputs are available on more than one

  ;; you can also write values to one of the channels non-deterministically

  (go (println :from-a (<! a)))
  (go (println :from-b (<! b)))
  
  (go (async/alts! [[a :put-into-a] [b :put-into-b]]))


  ;; or mix and match reading and writing
  (go (>! a :work))
  (go (println (<! b)))
  (go (println (async/alts! [a [b :on-my-break]])))


  ;; this is useful in combination with timeouts:
  ;; timeouts are special channels,
  ;; which close themselves after a certain amount of time

  (do
    (def c (async/timeout 2000))
    (go (>! c :yo))
    (go (println :got1 (<! c)))
    (go (println :got2 (<! c))))

  (def c (chan))

  ;; Read a value from c or give up after three seconds:
  (go (println (async/alts! [c (async/timeout 3000)])))

  ;; c could, for example,
  ;; receive the response to an asynchronous web request.

  ;; Rendezvous on c or give up after 3 seconds.
  (go (println (async/alts! [[c :are-you-there?] (async/timeout 3000)])))




  ;; non-blocking alt:

  (go (println (async/alts! [a b]
                            :default :im-so-alone)))


  (go (>! a :high-five!))
  (go (println (async/alts! [a b]
                            :default :left-hanging)))




  ;; Can you construct a buffer now?

  (def left (chan))
  (def right (chan))

  (defn buffer [n]
    (async/go-loop [coll []]
      (if (empty? coll)
         (recur (cons (<! left) coll)) ;; I/O
         (let [cands (into [[right (last coll)]]
                           (when (< (count coll) n) [left]))
               [v c] (async/alts! cands :priority true)] ;; I/O
           ;; :priority true maintains the order of incoming elements
           (if (= left c)
             (recur (cons v coll))
             (recur (butlast coll)))))))


  (buffer 3)

  (defn put-buf [v]
    (go (>! left v)))

  (defn take-buf []
    (go (println :taken (<! right))))

  (put-buf 1)
  (put-buf 2)
  (take-buf)
  (take-buf)
  (take-buf)
  (put-buf :x)

  ;; what is the difference with this happening on a channel?



  (defn put-buf2 [v]
    (go (>! left v) (println :done)))

  (put-buf2 :a)
  (put-buf2 :b)
  (put-buf2 :c)
  (put-buf2 :d) ;; not :done
  (take-buf)
  (take-buf)

  ;; vs.

  (def c (chan))
  (go (>! c :a) (println :done-a))
  (go (>! c :b) (println :done-b))
  (go (println :taken (<! c)))
  (go (println :taken (<! c)))



  ;; The buffer defined above is complex and confusing.
  ;; For this reason, a buffer size can be directly assigned to a channel:


  (def c (chan 3))

  (go (>! c :a) (println :done-a))
  (go (>! c :b) (println :done-b))
  (go (println :taken (<! c)))
  (go (println :taken (<! c)))



  ;; Sometimes it's okay if not all values are processed.


  (def c (chan (async/dropping-buffer 1)))
  (go (>! c 1))
  (go (>! c 2))
  (go (>! c 3))
  (go (println (<! c)))
  (go (println (async/alts! [c] :default :nothing-here)))


  ;; dropping buffers take and infinite amount of inputs,
  ;; but simply discard everything if the buffer is full.
  ;; Use Case: Load balancing in the system, combine with timeouts



  (def c (chan (async/sliding-buffer 2)))
  (go (>! c :x))
  (go (>! c :y))
  (go (>! c :z))
  (go (println (<! c)))
  (go (println (<! c)))

  ;; sliding buffer discard old values if new ones are input.
  ;; Use Case: Sensors that can write to a channel at high frequency,
  ;; of which you want always the most recent values





  ;; Use case: Serialization of accesses to shared resources


  (dotimes [_ 3]
    (future (println "Gallia est omnis divisa in partes tres,"
                     "quarum unam incolunt Belgae,"
                     "aliam Aquitani,"
                     "tertiam qui ipsorum lingua Celtae,"
                     "nostra Galli appellantur.")))


  (def c (chan))
  (async/go-loop []
                 (when-let [v (<! c)]
                   (apply println v)
                   (recur)))
  (dotimes [_ 3]
    (>!! c ["Gallia est omnis divisa in partes tres,"
            "quarum unam incolunt Belgae,"
            "aliam Aquitani,"
            "tertiam qui ipsorum lingua Celtae,"
            "nostra Galli appellantur."]))
  (async/close! c)



  ;; Duplication of messages

  (def c (chan 5))
  (def cc (chan 5))
  (def ccc (chan 5))

  (def m (async/mult c))
  ;; everything that is written to c is copied to the registered channels

  (async/tap m cc)
  (async/tap m ccc)
  ;; register cc and ccc

  (go (>! c 9))

  (go (println (<! cc)))
  (go (println (<! ccc)))


  ;; But: All tapping channels must read the value,
  ;; before the next is distrubuted
  ;; Buffers can help here.


  ;; publish-subscribe
  ;; fast mult + filter

  (def c (chan 2))
  (def p (async/pub c :country))


  (def usa-chan (chan 2))
  (async/sub p :usa usa-chan)

  (def ger-chan (chan 2))
  (async/sub p :germany ger-chan)

  (go (>! c {:city :miami, :country :usa, :weather :sunny}))
  (go (>! c {:city :duesseldorf, :country :germany, :weather :meh}))
  (go (>! c {:city :los-angeles, :country :usa, :weather :forest-fire}))
  (go (>! c {:city :munich, :country :germany, :weather :rain}))

  (go (println :german-weather (<! ger-chan)))
  (go (println :usa-weather (<! usa-chan)))



  ;; In combination with transducers

  (def c (chan 5 (partition-by even?)))
  (go (>! c 1))

  (go (println (<! c)))

  (go (>! c 3))
  (go (>! c 2))

  (go (println (<! c)))

  (async/close! c)


  ;; pipelines take two channels and a transducer:
  ;; values are taken from one channel,
  ;; processed by the transducer
  ;; and the result put on the other channel

  (def a (chan))
  (def b (chan))

  (async/pipeline 1 b (map inc) a)


  (go (>! a 3))
  (<!! b)

  ;; in general, this is preferable to implementing a go-loop yourself






  ;; Why do you use core.async?
  ;; - Make callbacks in Clojurescript tolerable:
  ;;     some libraries even expect channels
  ;; - Separate components with simple data flow:
  ;;     channels are "somewhat" like queues.
  ;;     If you have a pipeline between components,
  ;;     core.async is very suitable.
  ;; - I/O (and <!, >! and alts! are exactly that)
  ;;   should be used in the outer layer of a component only
  ;;   and not spread across the entirety of the code.

  ;; channels are state.
  ;; If every function uses a channel (= state),
  ;; everything functional about the program is lost.

  ;; Asynchronous operations imply non-determinism.
  ;; Channels often make it easier for us to understand a program,
  ;; but make is difficult to test.
  ;; If you can avoid asynchronous operations
  ;; (performance is sufficient, you don't need it, ...),
  ;; then you should avoid it!



  )
