(ns braid.server.db.invitation
  (:require [datomic.api :as d]
            [braid.server.db :as db]
            [braid.server.db.common :refer :all]))

;; Queries

(defn invite-by-id
  [invite-id]
  (some-> (d/pull (db/db)
                  [:invite/id
                   {:invite/from [:user/id :user/email :user/nickname]}
                   :invite/to
                   {:invite/group [:group/id :group/name]}]
                  [:invite/id invite-id])
          db->invitation))

(defn invites-for-user
  [user-id]
  (->> (d/q '[:find (pull ?i [{:invite/group [:group/id :group/name]}
                              {:invite/from [:user/id :user/email :user/nickname]}
                              :invite/to
                              :invite/id])
              :in $ ?user-id
              :where
              [?u :user/id ?user-id]
              [?u :user/email ?email]
              [?i :invite/to ?email]]
            (db/db) user-id)
       (map (comp db->invitation first))))

;; Transactions

(defn create-invitation!
  [{:keys [id inviter-id invitee-email group-id]}]
  (->> {:invite/id id
        :invite/group [:group/id group-id]
        :invite/from [:user/id inviter-id]
        :invite/to invitee-email
        :invite/created-at (java.util.Date.)}
       (create-entity! db/conn)
       db->invitation))

(defn retract-invitation-txn
  [invite-id]
  [[:db.fn/retractEntity [:invite/id invite-id]]])
