(ns util.helper)

(defn abs [x]
  (Math/abs x))

(defn get-description [x]
  (cond
    (not (nil? (meta x))) (meta x)
    (nil? x) ""
    (number? x) x
    (string? x) x
    :else (if (class? (type x))
            (.getSimpleName (type x))
            (type x))))