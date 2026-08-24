import streamlit as st
import pandas as pd
import numpy as np
import joblib
import time
from datetime import datetime

# Configure web page
st.set_page_config(
    page_title="Jeevashraya Command Center",
    page_icon="🚨",
    layout="wide",
    initial_sidebar_state="expanded"
)

st.title(" Jeevashraya: Edge-AI Landslide Early Warning Command Center")
st.caption("Real-time slope telemetry and causal ML inference dashboard for early shear-failure detection and mesh alerting.")
# Load your Colab-trained model
@st.cache_resource
def load_ml_model():
    return joblib.load('landslide_model.pkl')

model = load_ml_model()

# Sidebar Controls
st.sidebar.header("⚙️ Simulation Controls")
st.sidebar.info("Adjust sliders below to test how the ML model predicts landslide risk live.")

sim_tilt = st.sidebar.slider("Simulate Tilt Delta (°)", 0.0, 60.0, 0.5, step=0.5)
sim_pres = st.sidebar.slider("Simulate Pressure Delta (hPa)", 0.0, 10.0, 0.1, step=0.1)

st.sidebar.markdown("---")
st.sidebar.subheader("📡 Mesh Network Status")
st.sidebar.success("● Scout Node: Online (ESP32)")
st.sidebar.info("● Mesh Protocol: ESP-NOW (2.4 GHz)")
st.sidebar.success("● Speaker Node: Synced (ESP8266)")

# Top Metric Cards
col1, col2, col3, col4 = st.columns(4)
m_tilt = col1.empty()
m_pres = col2.empty()
m_risk = col3.empty()
m_level = col4.empty()

status_banner = st.empty()

tab1, tab2 = st.tabs(["📊 Live Movement Graphs", "📋 Hazard Incident Logs"])
with tab1:
    cg1, cg2 = st.columns(2)
    chart_tilt = cg1.empty()
    chart_pres = cg2.empty()
with tab2:
    log_table = st.empty()

# Rolling History Buffer
if 'history' not in st.session_state:
    st.session_state.history = pd.DataFrame(columns=['TiltDiff', 'PressureDiff'])
if 'incidents' not in st.session_state:
    st.session_state.incidents = []

# Continuous Live ML Loop
while True:
    tilt_diff = sim_tilt
    pressure_diff = sim_pres

    # Update Rolling Dataframe
    new_point = pd.DataFrame([[tilt_diff, pressure_diff]], columns=['TiltDiff', 'PressureDiff'])
    st.session_state.history = pd.concat([st.session_state.history, new_point], ignore_index=True).tail(30)

    # Compute Exact Model Features
    tilt_ema = st.session_state.history['TiltDiff'].ewm(span=4, adjust=False).mean().iloc[-1]
    pres_ema = st.session_state.history['PressureDiff'].ewm(span=4, adjust=False).mean().iloc[-1]
    kinetic_power = tilt_ema * pres_ema

    feature_vector = pd.DataFrame([[tilt_diff, pressure_diff, tilt_ema, pres_ema, kinetic_power]],
                                  columns=['TiltDiff', 'PressureDiff', 'TiltDiff_EMA', 'PressureDiff_EMA', 'KineticPower'])

    # Predict Risk Probability
    probabilities = model.predict_proba(feature_vector)[0]
    classes = list(model.classes_)
    alert_idx = classes.index('ALERT') if 'ALERT' in classes else 1
    risk_score = probabilities[alert_idx] * 100

    # Display Metrics
    m_tilt.metric("Tilt Shift", f"{tilt_diff:.2f}°")
    m_pres.metric("Pressure Delta", f"{pressure_diff:.2f} hPa")
    m_risk.metric("Hazard Risk Score", f"{risk_score:.1f}%")

    if risk_score > 70:
        m_level.metric("Intensity Tier", "🔴 CRITICAL")
        status_banner.error(f"🚨 CRITICAL ALERT: High-Velocity Shear Failure Detected ({risk_score:.1f}% Risk) — Siren Triggered via ESP-NOW")
        st.session_state.incidents.append({"Time": datetime.now().strftime("%H:%M:%S"), "Status": "CRITICAL COLLAPSE", "Risk": f"{risk_score:.1f}%"})
    elif risk_score > 35 or tilt_diff > 12.0:
        m_level.metric("Intensity Tier", "🟡 FILTERED / CREEP")
        status_banner.warning(f"⚠️ ANOMALY FILTERED: Transient Vibration / Surface Creep ({risk_score:.1f}% Risk)")
    else:
        m_level.metric("Intensity Tier", "🟢 SAFE")
        status_banner.success(f"✅ STATUS: SAFE — Normal Baseline Stability ({risk_score:.1f}% Risk)")

    with tab1:
        chart_tilt.line_chart(st.session_state.history['TiltDiff'], height=220)
        chart_pres.line_chart(st.session_state.history['PressureDiff'], height=220)

    with tab2:
        if st.session_state.incidents:
            log_table.dataframe(pd.DataFrame(st.session_state.incidents).tail(10), use_container_width=True)

    time.sleep(0.3)
