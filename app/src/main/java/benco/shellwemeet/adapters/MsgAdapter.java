package benco.shellwemeet.adapters;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.backendless.Backendless;
import com.backendless.messaging.PublishMessageInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import benco.shellwemeet.R;

public class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.ViewHolder>{

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView msgText;
        TextView msgTimeStemp;

        View msgContainer;


        public ViewHolder(View itemView) {
            super(itemView);
            // setting the ui elements from the itemView (which will be R.layout.message_cell)
            msgText = itemView.findViewById(R.id.msgCellmsgText);
            msgTimeStemp = itemView.findViewById(R.id.msgCellmsgTimeStampTxt);
            msgContainer = itemView.findViewById(R.id.msgContainer);
        }
    }

    Context context;
    List<PublishMessageInfo> msgsList;

    public MsgAdapter(Context context, List<PublishMessageInfo> msgsList) {
        this.context = context;
        this.msgsList = msgsList;
    }

    @NonNull
    @Override
    public MsgAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //called when a viewHolder is created

        //getting view holding our cell's data
        LayoutInflater inflater = LayoutInflater.from(context);
        View userView = inflater.inflate(R.layout.message_cell, parent, false);

        //make a viewHolder from the cell's layout
        MsgAdapter.ViewHolder viewHolder = new MsgAdapter.ViewHolder(userView);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //called when a viewHolder is bound to a data item (in our case - a message)

        //get a single message from the list according to the current position
        PublishMessageInfo message = msgsList.get(position);

        holder.msgText.setText((String)message.getMessage());
        Date date = new Date(message.getTimestamp());
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        holder.msgTimeStemp.setText(sdf.format(date));


        LinearLayout.LayoutParams paramsMsg = new LinearLayout.
                LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

        if(message.getPublisherId().equals(Backendless.UserService.loggedInUser())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                holder.msgContainer.setBackgroundColor(context.getResources().getColor(R.color.myChatTxt, null));

            } else {
                holder.msgContainer.setBackgroundColor(context.getResources().getColor(R.color.myChatTxt));
            }
            paramsMsg.gravity = Gravity.END;
            holder.msgContainer.setLayoutParams(paramsMsg);

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                holder.msgContainer.setBackgroundColor(context.getResources().getColor(R.color.yourChatTxt, null));
            } else {
                holder.msgContainer.setBackgroundColor(context.getResources().getColor(R.color.yourChatTxt));
            }
            paramsMsg.gravity = Gravity.START;
            holder.msgContainer.setLayoutParams(paramsMsg);

        }

    }

    @Override
    public int getItemCount() {
        return msgsList.size();
    }
}
