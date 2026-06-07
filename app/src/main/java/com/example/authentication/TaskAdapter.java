package com.example.authentication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private Context context;
    private ArrayList<TaskModel> taskList;
    private CollectionReference tasksRef;

    public TaskAdapter(Context context, ArrayList<TaskModel> taskList) {
        this.context = context;
        this.taskList = taskList;
        // Point reference pointing directly to the task documents collection path
        this.tasksRef = FirebaseFirestore.getInstance().collection("maintenance_tasks");
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskModel task = taskList.get(position);

        holder.tvStation.setText("Station Target: " + task.getTargetStation());
        holder.tvPhone.setText("Specialist Phone: " + task.getSpecialistPhone());
        holder.tvDate.setText("Operation Date: " + task.getDate());

        // CRUD: Open Dialog to execute UPDATE action
        holder.btnUpdate.setOnClickListener(v -> showUpdateDialog(task));

        // CRUD: Trigger live DELETE operation query
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Task")
                    .setMessage("Remove this scheduled maintenance task assignment?")
                    .setPositiveButton("DELETE", (dialog, which) -> {
                        tasksRef.document(task.getTaskId()).delete()
                                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Task record deleted successfully!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(context, "Delete operation failed.", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    // CRUD: Inflates a dynamic form to Update entries inside Firestore
    private void showUpdateDialog(TaskModel task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Update Scheduled Task");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText etStation = new EditText(context);
        etStation.setText(task.getTargetStation());
        layout.addView(etStation);

        final EditText etPhone = new EditText(context);
        etPhone.setText(task.getSpecialistPhone());
        layout.addView(etPhone);

        builder.setView(layout);

        builder.setPositiveButton("SAVE CHANGES", (dialog, which) -> {
            String updatedStation = etStation.getText().toString().trim();
            String updatedPhone = etPhone.getText().toString().trim();

            if (updatedStation.isEmpty() || updatedPhone.isEmpty()) {
                Toast.makeText(context, "Fields cannot be blank!", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("targetStation", updatedStation);
            updates.put("specialistPhone", updatedPhone);

            // Execute the UPDATE command targeting this single document ID
            tasksRef.document(task.getTaskId()).update(updates)
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Database document updated!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(context, "Update query failed.", Toast.LENGTH_SHORT).show());
        });

        builder.setNegativeButton("CANCEL", null);
        builder.show();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvStation, tvPhone, tvDate;
        Button btnUpdate, btnDelete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStation = itemView.findViewById(R.id.tvTaskStation);
            tvPhone = itemView.findViewById(R.id.tvTaskPhone);
            tvDate = itemView.findViewById(R.id.tvTaskDate);
            btnUpdate = itemView.findViewById(R.id.btnUpdateTask);
            btnDelete = itemView.findViewById(R.id.btnDeleteTask);
        }
    }
}