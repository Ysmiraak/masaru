(ns masaru.core)

(defn consume
  "Automaton A consumes symbol S at vertex V, disposing by D."
  ([A S V] (consume A S V nil))
  ([A S V D]
   (letfn [(process [s v]
             (->> (get-actions s v)
                  (map (partial act v))
                  join))
           
           (join [ms] (case (count ms) 0 nil 1 (first ms)
                            (apply merge-with into ms)))
           
           (stage [s v] (map #((A %) s) (keys (dissoc v :res))))
           
           (get-actions [s v] (reduce into (stage s v)))
           (goto [s v] (reduce #(assoc %1 %2 #{v}) {} (stage s v)))

           (get-prevs [vs] (reduce into (-> vs first (dissoc :res) vals)))
           
           (act [v a] (if (number? a)
                        {a #{v} :res (D S)}
                        (redus (list v) (pop a))))
           
           (redus [vs r]
             (if (= 1 (count r))
               (->> vs
                    (redus' (peek r))
                    (map (partial process S))
                    join)
               (let [ps (get-prevs vs)]
                 (if (= 1 (count ps))
                   (recur (conj vs (first ps)) (pop r))
                   (join (for [p ps] (redus (conj vs p) (pop r))))))))
           
           (redus' [s vs]
             (->> vs get-prevs
                  (map #(assoc (goto s %) :res (D s vs)))
                  ;; (map redus'')
                  ;; join
                  ))

           (redus'' [v]
             (let [as (->> (get-actions S v)
                           (filter vector?))]
               (if (empty? as)
                 v
                 (join (map (partial act v) as)))))]
     (process S V))))

;; NOTE on M: It must accept a list of vertices as arguments,
;; and return a set. E.g. (fn do-nothing [dv] #{})

(defn parse
  "Let states consume string with method. Returns the final vertex
  whose predecs are the possible parses, or nil if no parse found."
  [states disposition string]
  (loop [v {0 nil} string string]
    (when-not (nil? v)
      (if (empty? string)
        (consume states :$ v disposition)
        (recur (consume states (first string) v disposition)
               (rest string))))))

(defn parse-forest
  "Let states consumes string for building a parse forest."
  [states string]
  (letfn [(splice [v] (if (= 1 (count v)) (first v) v))
          (as-sexp
            ([s] [s])
            ([s vs]
             [(conj (map splice (map :res vs)) s)]))]
    (parse states as-sexp string)))

(defn draw-forest-as-sexp
  "Draw forest from vertex v as s-expression, where the or-nodes are
  represented as vectos."
  [v]
  (if (= :$ (:symbol v))
    (->> v :prevmap vals (reduce into) (mapv draw-forest-as-sexp))
    (case (count (:data? v))
      0 (:symbol v)
      1 (conj (apply map draw-forest-as-sexp (:data? v)) (:symbol v))
      (mapv #(conj (map draw-forest-as-sexp %) (:symbol v)) (:data? v)))))

(defn draw-forest-as-graph
  "todo"
  [v])

(defn print-in-dot
  "http://sandbox.kidstrythisathome.com/erdos/"
  [DAG]
  (println "digraph g {")
  (doseq [[fro tos] DAG to tos]
    (println (str \" fro \") "->" (str \" to \" \;)))
  (println \}))
