import React, { useState } from "react";
import MessageTab from "./messages/MessageTab";
import MessageList from "./messages/MessageList";
import NewFriendsList from "./messages/NewFriendsList";

const MessageSidebar = () => {
  const [activeTab, setActiveTab] = useState<"messages" | "new friends">(
    "messages"
  );

  const handleActiveTab = (tab: "messages" | "new friends") => {
    setActiveTab(tab);
    console.log(tab);
  };

  return (
    <div>
      <MessageTab onClick={handleActiveTab} />
      <div className="mt-10">
        {activeTab === "messages" ? <MessageList /> : <NewFriendsList />}
      </div>
    </div>
  );
};

export default MessageSidebar;
