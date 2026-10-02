import streamlit as st
import pandas as pd

st.set_page_config(page_title="PDF to Excel Converter", layout="centered")
st.title("📄 Automated Document Data Entry Tool")
st.write("Upload scanned batches or PDFs below to automatically generate structured Excel sheets.")

uploaded_files = st.file_uploader(
    "Choose PDF or Image files",
    type=["pdf", "png", "jpg"],
    accept_multiple_files=True
)

if uploaded_files:
    st.success(f"Successfully loaded {len(uploaded_files)} document(s).")
    
    if st.button("Process Documents to Excel"):
        with st.spinner("Extracting parameters and compiling data..."):
            # Structured simulation placeholder representing OCR extraction output
            extracted_data = [
                {"File Reference": "Doc_001.pdf", "Date": "2026-10-02", "Particulars": "Client Invoice A", "Amount": 12500},
                {"File Reference": "Doc_002.pdf", "Date": "2026-10-02", "Particulars": "Client Invoice B", "Amount": 8400},
                {"File Reference": "Doc_003.pdf", "Date": "2026-10-03", "Particulars": "Supplier Bill C", "Amount": 3200}
            ]
            df = pd.DataFrame(extracted_data)
        
        st.subheader("Extracted Data Preview:")
        st.dataframe(df)
        
        output_path = "Master_Data_Output.xlsx"
        df.to_excel(output_path, index=False)
        
        with open(output_path, "rb") as f:
            st.download_button(
                label="📥 Download Master Excel Sheet",
                data=f,
                file_name="Processed_Records.xlsx",
                mime="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
